package org.jb.evaluator.impl;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jb.ast.api.ASTNode;
import org.jb.ast.api.DeclStatement;
import org.jb.ast.api.Expr;
import org.jb.ast.api.FloatLiteral;
import org.jb.ast.api.*;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.ast.diagnostics.DiagnosticListener;

/**
 *
 * @author vkvashin
 */
public class EvaluatorImpl {
    
    private final Appendable out;
    private final DiagnosticListener diagnosticListener;
    
    // Thread safety: we assume that all fields in variables are accessed from the single executor's thread
    // This is asserted in several places.
    // When we introduce multy-threaded calculations, we'll probably need synchronization.
    // But right now it will only slow things down.
    private Thread execurorThread;
    
    private Symtab symtab;
    
    private static final boolean TRACE = Boolean.getBoolean("jbs.trace");
    
    /** There are some errors that should be already reported by parser; the question is whether to report them  */
    private static final boolean REPORT_PARSER_ERRORS = false;

    public EvaluatorImpl(Appendable out, DiagnosticListener diagnosticListener) {
        this.out = out;
        this.diagnosticListener = diagnosticListener;
        symtab = new Symtab(null, false);
    }

    public void execute(ASTNode ast) {
        execurorThread = Thread.currentThread();
        for( ASTNode node = ast; node != null; node = node.getNextSibling()) {
            executeImpl(node);
        }
    }

    private void executeImpl(ASTNode ast) {
        switch (ast.getNodeKind()) {            
            case DECL:
                if (ast instanceof DeclStatement) {
                    executeDecl((DeclStatement) ast);                    
                } else { // should never ever happen
                    error(ast, "unexpected statement class: " + ast);
                }
                break;
            case OUT:
                if (ast instanceof OutStatement) {
                    executeOut((OutStatement) ast);
                } else {
                    error(ast, "unexpected statement class: " + ast);
                }
                break;
            case PRINT:
                if (ast instanceof PrintStatement) {
                    executePrint((PrintStatement) ast);
                } else {
                    error(ast, "unexpected statement class: " + ast);
                }
                break;
            default:
                error(ast, "can not execute statement: " + ast.toString());
        }
    }

    private void executeDecl(DeclStatement stmt) {
        String name = stmt.getName().toString();
        if (REPORT_PARSER_ERRORS && symtab.contains(name)) {
            error(stmt, "duplicate variable declaration " + name);
        }
        Variable var = new Variable(name, stmt);
        symtab.put(name, var);
    }

    private void executePrint(PrintStatement stmt) {
        print(stmt.getString().getText());
        print("\n");
    }
    
    private void executeOut(OutStatement stmt) {
        Expr expr = stmt.getExpr();
        Value value = evaluate(expr);
        print(value);
        print("\n");
    }
    
    private void print(CharSequence text) {
        try {
            out.append(text);
        } catch (IOException ex) {
            error("can not write to output: " + ex.getLocalizedMessage());
        }
    }

    private void print(int value) {
        print(Integer.toString(value));
    }
    
    private void print(float value) {
        print(Float.toString(value));
    }
    
    private void print(Value value) {
        final Type type = value.getType();
        switch (type) {
            case INT:
                print(value.getInt());
                break;
            case FLOAT:
                print(value.getFloat());
                break;
            case SEQ_INT:
                {
                    boolean first = true;
                    for (int i : value.getIntArray()) {
                        if (first) {
                            first = false;
                        } else {
                            print(", ");
                        }
                        print(i);
                    }
                }
                break;
            case SEQ_FLOAT:
                {
                    boolean first = true;
                    for (float f : value.getFloatArray()) {
                        if (first) {
                            first = false;
                        } else {
                            print(", ");
                        }
                        print(f);
                    }
                }
                break;
            case ERRONEOUS:
                break; // do nothing, somebody should have already reported this
            case STRING:
            default:
                error("unexpected value type: " + type);
        }
    }

    /** Evaluates expression; NB: Never returns null. */
    private Value evaluate(Expr expr) {
        switch (expr.getNodeKind()) {
            case PAREN:
                return evaluate(((ParenExpr) expr).getFirstChild());
            case INT:
                try {
                    return new Value(Integer.parseInt(((Literal) expr).getText().toString()));
                } catch (NumberFormatException ex) {
                    error(expr, ex.getLocalizedMessage());
                    return Value.ERROR;
                }
            case FLOAT:
                try {
                    return new Value(Float.parseFloat(((Literal) expr).getText().toString()));
                } catch (NumberFormatException ex) {
                    error(expr, ex.getLocalizedMessage());
                    return Value.ERROR;
                }
            case ID:
                IdExpr id = (IdExpr) expr;
                Variable var = symtab.get(id.getName().toString());
                if (var != null) {
                    return var.getValue();
                } else {                    
                    if (REPORT_PARSER_ERRORS) {
                        error(expr, "undeclared variable " + id.getName());
                    }
                    return Value.ERROR;
                }
            case OP:
                return evaluateOperation((BinaryOpExpr) expr);
            case SEQ:
                return evaluateSequence((SeqExpr) expr);
            case MAP:
                throw new UnsupportedOperationException("Map is not supported yet");
            case REDUCE:
                throw new UnsupportedOperationException("reduce is not supported yet");
            case STRING:
            case DECL:
            case OUT:
            case PRINT:
            default:
                error(expr, "can not evaluate node: " + expr.getType());
                return Value.ERROR;
        }
    }

    private Value evaluateSequence(SeqExpr expr) {
        Expr firstExpr = expr.getFirst();
        Expr lastExpr = expr.getLast();
        if (firstExpr != null && lastExpr != null) {
            Value firstValue = evaluate(firstExpr);
            if (firstValue.getType() == Type.INT) {
                Value lastValue = evaluate(lastExpr);
                if (lastValue.getType() == Type.INT) {
                    int first = firstValue.getInt();
                    int last = lastValue.getInt();
                    int[] data = new int[last - first + 1];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = first + i;
                    }
                    return new Value(data);
                }
            }
        }
        return Value.ERROR;
    }

    private Value evaluateOperation(BinaryOpExpr expr) {
        Expr leftExpr = expr.getLeft();
        if (leftExpr != null) {
            Expr rightExpr = expr.getRight();
            if (rightExpr != null) {
                Value leftValue = evaluate(leftExpr);
                if (isArythmetic(leftValue)) {
                    Value rightValue = evaluate(rightExpr);
                    if (isArythmetic(rightValue)) {
                        return evaluateOperation(expr, leftValue, rightValue);
                    }
                }
            }
        }
        return Value.ERROR;
    }
    
    private boolean isArythmetic(Value value) {
        return value.getType() == Type.INT || value.getType() == Type.FLOAT;
    }
        
    private Value evaluateOperation(BinaryOpExpr expr, Value leftValue, Value rightValue) {
        final BinaryOpExpr.OpKind op = expr.getOpKind();
        assert isArythmetic(leftValue); // guaranteed by caller
        assert isArythmetic(rightValue); // guaranteed by caller
        final Type leftType = leftValue.getType();
        final Type rightType = rightValue.getType();
        switch (op) {            
            case DIV:
                if (rightType == Type.INT && rightValue.getInt() == 0) {
                    error(expr, "zero division");
                    return Value.ERROR;
                }
            case ADD: 
            case SUB:
            case MUL:
                if(leftType == Type.INT && rightType == Type.INT) {
                    return new Value(evaluateOperation(op, leftValue.getInt(), rightValue.getInt()));
                } else if(leftType == Type.FLOAT && rightType == Type.FLOAT) {
                    return new Value(evaluateOperation(op, leftValue.getFloat(), rightValue.getFloat()));
                } else if(leftType == Type.INT && rightType == Type.FLOAT) {
                    return new Value(evaluateOperation(op, (float)leftValue.getInt(), rightValue.getFloat()));
                } else if(leftType == Type.FLOAT && rightType == Type.INT) {
                    return new Value(evaluateOperation(op, leftValue.getFloat(), (float)rightValue.getInt()));
                } else {
                    throw new AssertionError("sholuld never ever get here");
                }          
            case POW:
                if (rightType != Type.INT) {
                    return Value.ERROR;
                }
                if (rightValue.getInt() < 0) {
                    error(expr, "only nonnegative power is supported");
                    return Value.ERROR;
                }
                if(leftType == Type.INT) {
                    return new Value(evaluatePower(leftValue.getInt(), rightValue.getInt()));
                } else if(leftType == Type.FLOAT) {
                    return new Value(evaluatePower(leftValue.getFloat(), rightValue.getInt()));
                } else {
                    throw new AssertionError("sholuld never ever get here");
                }
            default:
                throw new AssertionError(op.name());
        }
    }

    private float evaluatePower(float left, int right) {
        assert right >= 0;
        if (right < 0) {
            return 0;
        }
        float result = left;
        while (right-- > 1) {
            result *= left;
        }
        return result;
    }

    private int evaluatePower(int left, int right) {
        assert right >= 0;
        if (right < 0) {
            return 0;
        }
        int result = 1;
        while (right-- > 0) {
            result *= left;
        }
        return result;        
    }

    /** except for ^ (power) ! */
    private int evaluateOperation(BinaryOpExpr.OpKind op, int left, int right) {
        switch (op) {            
            case ADD:
                return left + right;
            case SUB:
                return left - right;
            case DIV:
                return left / right;
            case MUL:
                return left * right;
            case POW:
            default:
                throw new IllegalArgumentException(""+op);
        }
    }

    /** except for ^ (power) ! */
    private float evaluateOperation(BinaryOpExpr.OpKind op, float left, float right) {
        switch (op) {            
            case ADD:
                return left + right;
            case SUB:
                return left - right;
            case DIV:
                return left / right;
            case MUL:
                return left * right;
            case POW:
            default:
                throw new IllegalArgumentException(""+op);
        }
    }

    private void error(String message) {
        diagnosticListener.report(Diagnostic.error(0, 0, message));
    }

    private void error(ASTNode node, String message) {
        diagnosticListener.report(Diagnostic.error(node.getLine(), node.getColumn(), message));
    }
    
    private void pushSymtab(boolean transitive) {
        symtab =  new Symtab(symtab, transitive);
    }
    
    private void popSymtab() {
        assert symtab.previous != null;
        symtab = symtab.previous;
    }

    private class Symtab {

        private boolean transitive;
        private Symtab previous;
        private Map<String, Variable> data = new TreeMap<>();        

        public Symtab(Symtab previous, boolean transitive) {
            this.transitive = transitive;
            this.previous = previous;
        }

        public boolean contains(String name) {
            if (data.containsKey(name)) {
                return true;
            }
            if (transitive && previous != null) {
                return previous.contains(name);
            }
            return false;
        }
        
        public Variable get(String name) {
            Variable var = data.get(name);
            if (var == null && transitive && previous != null) {
                var = previous.get(name);
            }
            return var;
        }

        public void put(String name, Variable var) {
            data.put(name, var);
        }
    }

    /**
     * From performance perspective, it's questional whether its worth to have this class.
     * But from the code readability and reliability we'd better use it than bare Object.
     * So if profiler ever shows that this leads to a bottleneck, we'll (probably) drop it and use bare Object as value.
     */
    private static class Value {
        
        private static final Value ERROR = new Value();

        private final Object value;        

        /** use this only for ERRONEOUS types! */
        private Value() {
            this.value = null;
        }
        
        public Value(int value) {
            this.value = Integer.valueOf(value);
        }
        
        public Value(Integer value) {
            assert value != null;
            this.value = value;
        }

        public Value(float value) {
            this.value = Float.valueOf(value);
        }

        public Value(Float value) {
            assert value != null;
            this.value = value;
        }

        public Value(int[] value) {
            assert value != null;
            this.value = value;
        }

        public Value(float[] value) {
            assert value != null;
            this.value = value;
        }

        public Type getType() {
            if (value instanceof Integer) {
                return Type.INT;
            } else if (value instanceof Float) {
                return Type.FLOAT;
            } else if (value instanceof int[]) {
                return Type.SEQ_INT;
            } else if (value instanceof float[]) {
                return Type.SEQ_FLOAT;
            } else if (value == null) {
                return Type.ERRONEOUS;
            }
            throw new IllegalStateException("Unexpected vaue class: " + value);
        }
        
        public int getInt() {
            return ((Integer) value).intValue();
        }
        
        public float getFloat() {
            return ((Float) value).floatValue();
        }
        
        public int[] getIntArray() {
            return (int[]) value;
        }
        
        public float[] getFloatArray() {
            return (float[]) value;
        }

        @Override
        public String toString() {
            return (value == null) ? "<error>" : value.toString();
        }        
    }
    
    private class Variable {

        private final String name;
        //private Type type;
        private final DeclStatement decl;
        private Value value;
        private boolean cached;

        protected Variable(String name, DeclStatement declaration) {
            this.name = name;
            this.decl = declaration;
        }

        public String getName() {
            return name;
        }

        public Value getValue() {
            assert Thread.currentThread() == execurorThread;
            if (!cached) {
                value = evaluate(decl.getInitializer());
                cached = true;
            }
            return value;
        }
    }
}
