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
    private final DiagnosticListener[] diagnosticListeners;
    
    // Thread safety: we assume that all fields in variables are accessed from the single executor's thread
    // This is asserted in several places.
    // When we introduce multy-threaded calculations, we'll probably need synchronization.
    // But right now it will only slow things down.
    private Thread execurorThread;
    
    private Symtab symtab;
    
    private static final boolean TRACE = Boolean.getBoolean("jbs.trace");
    private static final boolean SUPPRESS_PREPARED_EXPRESSIONS = Boolean.getBoolean("jbs.suppress.prepare.expressions");
    private static final boolean CATCH_PREDPARED_EXCEPTION = Boolean.getBoolean("jbs.catch.expressions");
    
    /** There are some errors that should be already reported by parser; the question is whether to report them  */
    private static final boolean REPORT_PARSER_ERRORS = false;

    public EvaluatorImpl(Appendable out, DiagnosticListener... diagnosticListeners) {
        this.out = out;
        this.diagnosticListeners = diagnosticListeners;
        symtab = new Symtab(null, false);
    }

    public void execute(ASTNode ast) {
        execurorThread = Thread.currentThread();
        for( ASTNode node = ast; node != null && ! Thread.currentThread().isInterrupted(); node = node.getNextSibling()) {
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
        if (REPORT_PARSER_ERRORS && symtab.contains(stmt)) {
            error(stmt, "duplicate variable declaration " + stmt.getDelarationName());
        }
        Variable var = new Variable(stmt);
        symtab.put(var);
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
    
    private void print(double value) {
        print(Double.toString(value));
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
                    for (double f : value.getFloatArray()) {
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
                return Value.create(((IntLiteral) expr).getValue());
            case FLOAT:
                return Value.create(((FloatLiteral) expr).getValue());
            case ID:
                IdExpr id = (IdExpr) expr;
                Variable var = symtab.get(id.getName());
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
                return evaluateMap((MapExpr)expr);
            case REDUCE:
                return evaluateReduce((ReduceExpr)expr);
            case STRING:
            case DECL:
            case OUT:
            case PRINT:
            default:
                error(expr, "can not evaluate node: " + expr.getType());
                return Value.ERROR;
        }
    }
    
    private Value evaluateMap(MapExpr expr) {
        Expr seqExpr = expr.getSequence();
        DeclStatement var = expr.getVar();
        Expr transformation = expr.getTransformation();
        // if either of the below is null, this should have already been reported
        if (seqExpr != null && var != null && transformation != null) {
            Value seqValue = evaluate(seqExpr);
            Type type = seqValue.getType();
            switch (type) {
                case SEQ_INT:
                    return evaluateMap(seqValue.getIntArray(), var, transformation);
                case SEQ_FLOAT:
                    return evaluateMap(seqValue.getFloatArray(), var, transformation);
                case ERRONEOUS:
                    return Value.ERROR;
                case INT:
                case FLOAT:
                case STRING:
                default:
                    error(expr, "unexpected type, expected sequence, but got " + type);
                    return Value.ERROR;
            }
        }
        return Value.ERROR;
    }

    private Value evaluateMap(Object array, DeclStatement varDecl, Expr transformation) {
        assert (array instanceof int[] || array instanceof double[]);
        // input arrays and its size
        // NB: as to input arrays, we always ise intIn if (array instanceof int[]), otherwise float.
        int[] intIn;
        double[] floatIn;
        final int size;
        if (array instanceof int[]) {
            intIn = (int[]) array;
            size = intIn.length;
            floatIn = null;
        } else {
            floatIn = (double[]) array;
            size = floatIn.length;
            intIn = null;
        }
        // With input arrays we sometimes have to switch to double[] and vice versa.
        // So we always initially try using int[] and switch to double[] as soon as we get float result
        int[] intOut = new int[size];
        double[] floatOut = null;   
        // smart variable:
        int[] idx = new int[1]; // to be able to use mutable index in anonimous class
        Value smartValue = new Value() {
            @Override
            public Type getType() {
                return (intIn != null) ? Type.INT : Type.FLOAT;
            }
            @Override
            public int getInt() {
                return intIn[idx[0]];
            }
            @Override
            public double getFloat() {
                return floatIn[idx[0]];
            }
        };
        Variable smartVar = new Variable(varDecl) {
            @Override
            public Value getValue() {
                return smartValue;
            }
        };
        pushSymtab(false);
        symtab.put(smartVar);
        try {
            Producer<Value> valueProducer = prepareExpressionIfPossible(transformation);
            for (idx[0] = 0; idx[0] < size; idx[0]++) {
                if (idx[0]%100==0 && Thread.currentThread().isInterrupted()) {
                    return Value.ERROR;
                }
                Value v = valueProducer.produce();
                Type type = v.getType();
                switch (type) {                    
                    case INT:
                        intOut[idx[0]] = v.getInt();
                        break;
                    case FLOAT:
                        if (floatOut == null) {
                            floatOut = new double[intOut.length];
                            for (int i = 0; i < idx[0]; i++) {
                                floatOut[i] = (double) intOut[i];
                            }
                            intOut = null; 
                        }
                        floatOut[idx[0]] = v.getFloat();
                        break;
                    case ERRONEOUS:
                        return Value.ERROR;
                    case SEQ_INT:
                    case SEQ_FLOAT:
                    case STRING:
                    default:
                        error(transformation, "unexpected type: " + type);
                        return Value.ERROR;
                }
            }
            return (floatOut == null) ? Value.create(intOut) : Value.create(floatOut);
        } finally {
            popSymtab();
        }
    }

    private Value evaluateReduce(ReduceExpr expr) {
        final Expr seqExpr = expr.getSequence();
        final Expr defValueExpr = expr.getDefValue();        
        final DeclStatement prev = expr.getPrev();
        final DeclStatement curr = expr.getCurr();        
        final Expr transformation = expr.getTransformation();
        // if either of the below is null, this should have already been reported
        if (seqExpr != null && defValueExpr != null && prev != null && curr != null && transformation != null) {
            Value defValue = evaluate(defValueExpr);
            if (!isArithmetic(defValue)) {
                error(defValueExpr, "expression should be either int or float");
                return Value.ERROR;
            }
            Value seqValue = evaluate(seqExpr);
            Type type = seqValue.getType();
            switch (type) {
                case SEQ_INT:
                    return evaluateReduce(seqValue.getIntArray(), defValue, prev, curr, transformation);
                case SEQ_FLOAT:
                    return evaluateReduce(seqValue.getFloatArray(), defValue, prev, curr, transformation);
                case ERRONEOUS:
                    return Value.ERROR;
                case INT:
                case FLOAT:
                case STRING:
                default:
                    error(expr, "unexpected type, expected sequence, but got " + type);
                    return Value.ERROR;
            }
        }
        return Value.ERROR;        
    }
    private Producer<Value> prepareExpressionIfPossible(Expr expr) {
        Op transfOp = SUPPRESS_PREPARED_EXPRESSIONS ? null : prepareExpr(expr);
        Producer<Value> valueProducer;
        if (transfOp != null) {
            if (transfOp.getReturnType() == Op.ReturnType.INT) {
                OpI transfOpI = (OpI) transfOp;
                valueProducer = () -> Value.create(transfOpI.eval());
            } else {
                OpF transfOpF = (OpF) transfOp;
                valueProducer = () -> Value.create(transfOpF.eval());
            }
        } else {
            valueProducer = () -> evaluate(expr);
        }
        return valueProducer;
    }

    private Value evaluateReduce(Object array, Value defValue, DeclStatement prevDecl, DeclStatement currDecl, Expr transformation) {
        assert isArithmetic(defValue);
        // input arrays and its size
        assert (array instanceof int[] || array instanceof double[]);
        // NB: as to input arrays, we always ise intIn if (array instanceof int[]), otherwise float.
        int[] intIn;
        double[] floatIn;
        final int size;
        if (array instanceof int[]) {
            intIn = (int[]) array;
            size = intIn.length;
            floatIn = null;
        } else {
            floatIn = (double[]) array;
            size = floatIn.length;
            intIn = null;
        }
        if (defValue.getType() == Type.INT) {
            if (array instanceof double[] || transformation.getType() == Type.FLOAT) {
                defValue = Value.create((double) defValue.getInt());
            }
        }
        // smart variables:
        Value[] accumulator = new Value[] { defValue }; // to be able to use mutable index in anonimous class
        Variable prevVar = new Variable(prevDecl) {
            @Override
            public Value getValue() {
                return accumulator[0];
            }
        };
        int[] idx = new int[1]; // to be able to use mutable index in anonimous class

        Value smartValue = new Value() {
            @Override
            public Type getType() {
                return (intIn != null) ? Type.INT : Type.FLOAT;
            }
            @Override
            public int getInt() {
                return intIn[idx[0]];
            }
            @Override
            public double getFloat() {
                return floatIn[idx[0]];
            }
        };
        Variable currVar = new Variable(currDecl) {
            @Override
            public Value getValue() {
                return smartValue;
            }
        };
        pushSymtab(false);
        symtab.put(prevVar);
        symtab.put(currVar);
        try {
            Producer<Value> valueProducer = prepareExpressionIfPossible(transformation);
            //Producer<Value> valueProducer = valueProducer = () -> evaluate(transformation);
            for (idx[0] = 0; idx[0] < size; idx[0]++) {
                if (idx[0]%100==0 && Thread.currentThread().isInterrupted()) {
                    return Value.ERROR;
                }         
                accumulator[0] = valueProducer.produce();
                if (accumulator[0].getType() == Type.ERRONEOUS) {
                    // upon error, don't waiste time in further calculations
                    return Value.ERROR;
                }
            }
            return accumulator[0];
        } finally {
            popSymtab();
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
                    return Value.create(data);
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
                if (isArithmetic(leftValue)) {
                    Value rightValue = evaluate(rightExpr);
                    if (isArithmetic(rightValue)) {
                        return evaluateOperation(expr, leftValue, rightValue);
                    }
                }
            }
        }
        return Value.ERROR;
    }
    
    private boolean isArithmetic(Value value) {
        return value.getType() == Type.INT || value.getType() == Type.FLOAT;
    }
        
    private Value evaluateOperation(BinaryOpExpr expr, Value leftValue, Value rightValue) {
        final BinaryOpExpr.OpKind op = expr.getOpKind();
        assert isArithmetic(leftValue); // guaranteed by caller
        assert isArithmetic(rightValue); // guaranteed by caller
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
                    return Value.create(evaluateOperation(op, leftValue.getInt(), rightValue.getInt()));
                } else if(leftType == Type.FLOAT && rightType == Type.FLOAT) {
                    return Value.create(evaluateOperation(op, leftValue.getFloat(), rightValue.getFloat()));
                } else if(leftType == Type.INT && rightType == Type.FLOAT) {
                    return Value.create(evaluateOperation(op, (float)leftValue.getInt(), rightValue.getFloat()));
                } else if(leftType == Type.FLOAT && rightType == Type.INT) {
                    return Value.create(evaluateOperation(op, leftValue.getFloat(), (float)rightValue.getInt()));
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
                    return Value.create(evaluatePower(leftValue.getInt(), rightValue.getInt()));
                } else if(leftType == Type.FLOAT) {
                    return Value.create(evaluatePower(leftValue.getFloat(), rightValue.getInt()));
                } else {
                    throw new AssertionError("sholuld never ever get here");
                }
            default:
                throw new AssertionError(op.name());
        }
    }

    private double evaluatePower(double left, int right) {
        return Math.pow(left, right);
    }

    private int evaluatePower(int left, int right) {
        if (left == -1) {
            return (right%2 == 0) ? +1 : -1;
        }
        return (int) Math.pow(left, right);
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
    private double evaluateOperation(BinaryOpExpr.OpKind op, double left, double right) {
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

    private void error(CharSequence message) {
        for (DiagnosticListener dl : diagnosticListeners) {
            dl.report(Diagnostic.error(0, 0, message));
        }
    }

    private void error(ASTNode node, CharSequence message) {
        for (DiagnosticListener dl : diagnosticListeners) {
            dl.report(Diagnostic.error(node.getLine(), node.getColumn(), message));
        }
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
        private Map<CharSequence, Variable> data = new TreeMap<>();        

        public Symtab(Symtab previous, boolean transitive) {
            this.transitive = transitive;
            this.previous = previous;
        }

        public boolean contains(DeclStatement stmt) {
            return contains(stmt.getDelarationName());
        }

        public boolean contains(CharSequence name) {
            if (data.containsKey(name)) {
                return true;
            }
            if (transitive && previous != null) {
                return previous.contains(name);
            }
            return false;
        }
        
        public Variable get(CharSequence name) {
            Variable var = data.get(name);
            if (var == null && transitive && previous != null) {
                var = previous.get(name);
            }
            return var;
        }

        public void put(Variable var) {
            data.put(var.getName(), var);
        }
    }

    /**
     * From performance perspective, it's questional whether its worth to have this class.
     * But from the code readability and reliability we'd better use it than bare Object.
     * So if profiler ever shows that this leads to a bottleneck, we'll (probably) drop it and use bare Object as value.
     */
    private static abstract class Value {
        
        private static final Value ERROR = new ErrorValue();

        public static Value create(int value) {
            return new IntValue(value);
        }

        public static Value create(int[] value) {
            return new IntArrayValue(value);
        }

        public static Value create(double value) {
            return new FloatValue(value);
        }

        public static Value create(double[] value) {
            return new FloatArrayValue(value);
        }

        public abstract Type getType();
        
        public int getInt() {
            throw new  UnsupportedOperationException();
        }
        
        public double getFloat() {
            throw new  UnsupportedOperationException();
        }
        
        public int[] getIntArray() {
            throw new  UnsupportedOperationException();
        }
        
        public double[] getFloatArray() {
            throw new  UnsupportedOperationException();
        }
    }

    private static final class ErrorValue extends Value{
        @Override
        public Type getType() {
            return Type.ERRONEOUS;
        }
    }
    
    private static final class IntValue extends Value {
        private final int value;
        public IntValue(int value) {
            this.value = value;
        }
        @Override
        public int getInt() {
            return value;
        }
        @Override
        public Type getType() {
            return Type.INT;
        }        
    }

    private static final class IntArrayValue extends Value {
        private final int[] value;
        public IntArrayValue(int[] value) {
            this.value = value;
        }
        @Override
        public int[] getIntArray() {
            return value;
        }
        @Override
        public Type getType() {
            return Type.SEQ_INT;
        }        
    }

    private static final class FloatValue extends Value {
        private final double value;
        public FloatValue(double value) {
            this.value = value;
        }
        @Override
        public double getFloat() {
            return value;
        }
        @Override
        public Type getType() {
            return Type.FLOAT;
        }        
    }
    
    private static final class FloatArrayValue extends Value {
        private final double[] value;
        public FloatArrayValue(double[] value) {
            this.value = value;
        }
        @Override
        public double[] getFloatArray() {
            return value;
        }
        @Override
        public Type getType() {
            return Type.SEQ_FLOAT;
        }        
    }
    
    private class Variable {

        private final CharSequence name;
        private final DeclStatement decl;
        private Value value;
        private boolean cached;

        protected Variable(DeclStatement declaration) {
            this.name = declaration.getDelarationName().toString();
            this.decl = declaration;
        }

        public CharSequence getName() {
            return name;
        }

        public DeclStatement getDeclaration() {
            return decl;
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

    private interface Producer<T> {
        T produce();
    }

    /**
     * Creates a tree of operations (Op descendants);
     * ops work much faster than traversing AST
     * @param expr
     * @return
     */
    private Op prepareExpr(Expr expr) {
        if (expr == null) {
            return null;
        }
        ASTNode.NodeKind nodeKind = expr.getNodeKind();
        switch (nodeKind) {
            case PAREN:
                return prepareExpr(((ParenExpr) expr).getFirstChild());
            case OP:
                BinaryOpExpr opExpr = (BinaryOpExpr) expr;
                final BinaryOpExpr.OpKind opKind = opExpr.getOpKind();
                final Op left = prepareExpr(opExpr.getLeft());
                final Op right = prepareExpr(opExpr.getRight());
                if (left == null || right == null) {
                    return null;
                }
                final Op.ReturnType lrt = left.getReturnType();
                final Op.ReturnType rrt = right.getReturnType();
                switch (opKind) {
                    case ADD:
                        if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.INT) {
                            return new AddII((OpI)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.FLOAT) {
                            return new AddFF((OpF)left, (OpF)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.INT) {
                            return new AddFI((OpF)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.FLOAT) {
                            return new AddIF((OpI)left, (OpF)right);
                        } else {
                            throw new IllegalStateException();
                        }
                    case SUB:
                        if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.INT) {
                            return new SubII((OpI)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.FLOAT) {
                            return new SubFF((OpF)left, (OpF)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.INT) {
                            return new SubFI((OpF)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.FLOAT) {
                            return new SubIF((OpI)left, (OpF)right);
                        } else {
                            throw new IllegalStateException();
                        }
                    case MUL:
                        if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.INT) {
                            return new MulII((OpI)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.FLOAT) {
                            return new MulFF((OpF)left, (OpF)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.INT) {
                            return new MulFI((OpF)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.FLOAT) {
                            return new MulIF((OpI)left, (OpF)right);
                        } else {
                            throw new IllegalStateException();
                        }
                    case DIV:
                        if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.INT) {
                            return new DivII((OpI)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.FLOAT) {
                            return new DivFF((OpF)left, (OpF)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.INT) {
                            return new DivFI((OpF)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.FLOAT) {
                            return new DivIF((OpI)left, (OpF)right);
                        } else {
                            throw new IllegalStateException();
                        }
                    case POW:
                        if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.INT) {
                            return new PowII((OpI)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.FLOAT) {
                            return new PowFF((OpF)left, (OpF)right);
                        } else if (lrt == Op.ReturnType.FLOAT && rrt == Op.ReturnType.INT) {
                            return new PowFI((OpF)left, (OpI)right);
                        } else if (lrt == Op.ReturnType.INT && rrt == Op.ReturnType.FLOAT) {
                            return new PowIF((OpI)left, (OpF)right);
                        } else {
                            throw new IllegalStateException();
                        }
                    default:
                        throw new AssertionError(opKind.name());
                }
            case ID:
                IdExpr idExpr = (IdExpr) expr;
                CharSequence name = idExpr.getName();
                Variable var = symtab.get(name);
                if (var == null) {
                    return null;
                }
                Value value = var.getValue();
                Type type = (value != null) ? value.getType() : idExpr.getType();
                if (type == Type.INT) {
                    return new VarI(var);
                } else if (type == Type.FLOAT) {
                    return new VarF(var);
                } else {
                    return null;
                }
            case INT:
                return new ConstI(((IntLiteral) expr).getValue());
            case FLOAT:
                return new ConstF(((FloatLiteral) expr).getValue());
            case DECL:
            case OUT:
            case PRINT:
            case SEQ:
            case MAP:
            case REDUCE:
            case STRING:
            default:
                return null;
        }
    }

    /**
     * Op descendants are operations like +, -, *, /
     * that work faster that traversing and evaluating AST tree.
     */

    private static abstract class Op {
        public enum ReturnType {
            INT,
            FLOAT
        }
        /** Use instead of instanceof checks */
        public abstract ReturnType getReturnType();
    }

    private static abstract class OpI extends Op {
        @Override
        public ReturnType getReturnType() {
            return Op.ReturnType.INT;
        }
        abstract public int eval();
    }

    private static abstract class OpF extends Op {
        @Override
        public ReturnType getReturnType() {
            return Op.ReturnType.FLOAT;
        }
        abstract public double eval();
    }

    private static final class ConstI extends OpI {
        private final int value;
        public ConstI(int value) {
            this.value = value;
        }
        @Override
        public int eval() {
            return value;
        }
    }

    private static final class ConstF extends OpF {
        private final double value;
        public ConstF(double value) {
            this.value = value;
        }
        @Override
        public double eval() {
            return value;
        }
    }

    private static abstract class OpII extends OpI {
        protected final OpI left;
        protected final OpI right;
        public OpII(OpI left, OpI right) {
            this.left = left;
            this.right = right;
        }
    }

    private static abstract class OpFF extends OpF {
        protected final OpF left;
        protected final OpF right;
        public OpFF(OpF left, OpF right) {
            this.left = left;
            this.right = right;
        }
    }

    private static abstract class OpIF extends OpF {
        protected final OpI left;
        protected final OpF right;
        public OpIF(OpI left, OpF right) {
            this.left = left;
            this.right = right;
        }
    }

    private static abstract class OpFI extends OpF {
        protected final OpF left;
        protected final OpI right;
        public OpFI(OpF left, OpI right) {
            this.left = left;
            this.right = right;
        }
    }

    private static final class AddII extends OpII {
        public AddII(OpI left, OpI right) {
            super(left, right);
        }
        @Override
        public int eval() {
            return left.eval() + right.eval();
        }
    }

    private static final class AddFF extends OpFF {
        public AddFF(OpF left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return left.eval() + right.eval();
        }
    }

    private static final class AddIF extends OpIF {
        public AddIF(OpI left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return (double) left.eval() + right.eval();
        }
    }

    private static final class AddFI extends OpFI {
        public AddFI(OpF left, OpI right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return left.eval() + (double) right.eval();
        }
    }

    private static final class SubII extends OpII {
        public SubII(OpI left, OpI right) {
            super(left, right);
        }
        @Override
        public int eval() {
            return left.eval() - right.eval();
        }
    }

    private static final class SubFF extends OpFF {
        public SubFF(OpF left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return left.eval() - right.eval();
        }
    }

    private static final class SubIF extends OpIF {
        public SubIF(OpI left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return (double) left.eval() - right.eval();
        }
    }

    private static final class SubFI extends OpFI {
        public SubFI(OpF left, OpI right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return left.eval() - (double) right.eval();
        }
    }

    private static final class MulII extends OpII {
        public MulII(OpI left, OpI right) {
            super(left, right);
        }
        @Override
        public int eval() {
            return left.eval() * right.eval();
        }
    }

    private static final class MulFF extends OpFF {
        public MulFF(OpF left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return left.eval() * right.eval();
        }
    }

    private static final class MulIF extends OpIF {
        public MulIF(OpI left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return (double) left.eval() * right.eval();
        }
    }

    private static final class MulFI extends OpFI {
        public MulFI(OpF left, OpI right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return left.eval() * (double) right.eval();
        }
    }

    private static final class DivII extends OpII {
        public DivII(OpI left, OpI right) {
            super(left, right);
        }
        @Override
        public int eval() {
            return left.eval() / right.eval();
        }
    }

    private static final class DivFF extends OpFF {
        public DivFF(OpF left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return left.eval() / right.eval();
        }
    }

    private static final class DivIF extends OpIF {
        public DivIF(OpI left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return (double) left.eval() / right.eval();
        }
    }

    private static final class DivFI extends OpFI {
        public DivFI(OpF left, OpI right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return left.eval() / (double) right.eval();
        }
    }

    private static final class PowII extends OpII {
        public PowII(OpI left, OpI right) {
            super(left, right);
        }
        @Override
        public int eval() {
            int l = this.left.eval();
            int r = this.right.eval();
            if (l == -1) {
                return (r % 2 == 0) ? +1 : -1;
            }
            return (int) Math.pow(l, r);
        }
    }

    private static final class PowFF extends OpFF {
        public PowFF(OpF left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return Math.pow(left.eval(), right.eval());
        }
    }

    private static final class PowIF extends OpIF {
        public PowIF(OpI left, OpF right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return Math.pow((double)left.eval(), right.eval());
        }
    }

    private static final class PowFI extends OpFI {
        public PowFI(OpF left, OpI right) {
            super(left, right);
        }
        @Override
        public double eval() {
            return Math.pow(left.eval(), (double)right.eval());
        }
    }

    private final class VarI extends OpI {
        private final Variable var;
        public VarI(Variable var) {
            this.var = var;
        }
        @Override
        public int eval() {
            Value value = var.getValue();
            if (value.getType() == Type.INT) { // just in case
                return var.getValue().getInt();
            } else {
                error(var.getDeclaration(), "unexpected type (expected int): " + value.getType());
                return 0;
            }
        }
    }

    private final class VarF extends OpF {
        private final Variable var;
        public VarF(Variable var) {
            this.var = var;
        }
        @Override
        public double eval() {
            Value value = var.getValue();
            if (value.getType() == Type.FLOAT) { // just in case
                return value.getFloat();
            } else {
                error(var.getDeclaration(), "unexpected type (expected float): " + value.getType());
                return 0;
            }
        }
    }
}
