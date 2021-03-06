package org.jb.parser.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Stack;
import java.util.Map;
import java.util.TreeMap;
import org.jb.ast.api.DeclStatement;
import org.jb.ast.api.*;
import org.jb.ast.diagnostics.DefaultDiagnosticListener;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.lexer.api.*;
import org.jb.ast.diagnostics.DiagnosticListener;

/**
 *
 * @author vkvashin
 */
public class ParserImpl {

    private final TokenBuffer tokens;
    private final DiagnosticListener diagnosticListener;
    private Symtab symtab;

    public ParserImpl(TokenStream ts, DiagnosticListener... errorListeners) throws TokenStreamException {
        tokens = new ZeroLookaheadTokenBuffer(ts); //WindowTokenBuffer(ts, 1, 4096);
        this.diagnosticListener = new CombinedDiagnosticListener(errorListeners);
        symtab = new Symtab(null, false);
    }

    public Statement parse() {
        Statement first;
        
        first = statement();

        Statement prev = first;
        Statement curr;
        while((curr = statement()) != null) {
            prev.setNextSibling(curr);
            prev = curr;
        }
        return first;
    }

    private void skipToTheNextLine() {
        Token tok = LA(0);
        if (!isEOF(tok)) {
            int line = tok.getLine();
            while (!isEOF(tok) && tok.getLine() == line) {
                consume();
                tok = LA(0);
            }
        }
    }

    private Statement statement() {
        while (true) {
            try {
                return statementImpl();
            } catch (SynaxError ex) {
                diagnosticListener.report(toDiagnostic(ex));
                skipToTheNextLine();
            }
        }
    }

    private Statement statementImpl() throws SynaxError {
        Token tok = LA(0);
        if (isEOF(tok)) {
            return null; // TODO: consider a special EOF token
        }
        switch (tok.getKind()) {
            case VAR:
                return declStatement();
            case PRINT:
                return printStatement();
            case OUT:
                return outStatement();
            default:
                throw new SynaxError(tok, "unexpected token " + tok.getText());
        }
    }

    private DeclStatement declStatement() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.VAR);
        final Token nameTok = LA(0);
        consume(); // variable name
        if (isEOF(nameTok) || nameTok.getKind() != Token.Kind.ID) {
            throw new SynaxError(LA(0), "var keyword should be followed by name");
        }
        if (LA(0).getKind() != Token.Kind.EQ) {
            throw new SynaxError(LA(0), "variable name should be followed by = sign");
        }
        consume();// =
        Expr expr = expression();
        Type type = expr.getType();
        CharSequence name = nameTok.getText();
        if (symtab.contains(name)) {
            error(nameTok, "duplicate variable declaration " + name);
        }
        symtab.put(name, type);
        return new DeclStatement(firstTok.getLine(), firstTok.getColumn(), name, expr);
    }

    private DeclStatement lambdaVarDecl(Type type) throws SynaxError {        
        final Token tok = LA(0);
        assert tok.getKind() == Token.Kind.ID;
        consume();
        CharSequence name = tok.getText();
        if (symtab.contains(name)) {
            error(tok, "duplicate variable declaration " + name);
        }
        symtab.put(name, type);
        return new DeclStatement(tok.getLine(), tok.getColumn(), tok.getText(), null);
    }

    private PrintStatement printStatement() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.PRINT);
        final Token stringTok = LA(0);
        consume(); // string
        if (stringTok.getKind() != Token.Kind.STRING) {
            throw new SynaxError(LA(0), "print keyword should be followed by a string");
        }
        StringLiteral sl = new StringLiteral(stringTok.getLine(), stringTok.getColumn(), stringTok.getText());
        return new PrintStatement(firstTok.getLine(), firstTok.getColumn(), sl);
    }

    private OutStatement outStatement() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.OUT);
        Expr expr = expression();
        return new OutStatement(firstTok.getLine(), firstTok.getColumn(), expr);
    }

    private Expr expression() throws SynaxError {
        Stack<BinaryOpExpr.OpKind> opStack = new Stack();
        Deque<Object> outQueue = new ArrayDeque<>(); // in fact only Expr and BinaryOpExpr.OpKind
        Token firstTok = LA(0);
        Expr operand = operand();
        outQueue.add(operand);
        while (isOperation(LA(0))) {
            BinaryOpExpr.OpKind currOp = getOpKind(LA(0));
            consume(); // operation
            BinaryOpExpr.OpKind stackOp = opStack.isEmpty() ? null : opStack.peek();
            while (stackOp != null && !stackOp.isWeaker(currOp)) {
                outQueue.add(opStack.pop());
                stackOp = opStack.isEmpty() ? null : opStack.peek();
            }
            opStack.push(currOp);
            operand = operand();
            outQueue.add(operand);
        }
        while(!opStack.isEmpty()) {
            outQueue.add(opStack.pop());
        }
        return expressionFromPN(outQueue, firstTok);
    }

    /**
     * Constructs an expression AST from a polish notation queue
     * @param q queue that contains expressions and operation kinds
     */
    private Expr expressionFromPN(Deque<Object> q, Token firstTok) {
        assert !q.isEmpty();
        Object o = q.pollLast();
        if (o instanceof Expr) {
            return (Expr) o;
        }
        BinaryOpExpr.OpKind op = (BinaryOpExpr.OpKind) o;
        Expr right = expressionFromPN(q, null);
        Expr left = expressionFromPN(q, null);
        int line = (firstTok == null) ? left.getLine() : firstTok.getLine();
        int column = (firstTok == null) ? left.getColumn(): firstTok.getColumn();
        return new BinaryOpExpr(line, column, op, left, right, diagnosticListener);
    }
    
    private Expr operand() throws SynaxError {
        final Token firstTok = LA(0);
        if (isEOF(firstTok)) {
            throw new SynaxError(firstTok, "unexpected end of file: expected expression");
        }
        switch (firstTok.getKind()) {
            case INT:
                return intLiteral();
            case FLOAT:
                return floatLiteral();
            case STRING:
                return stringLiteral();
            case ID:
                return id();
            case LPAREN:
                return paren();
            case LCURLY:
                return seq();
            case MAP:
                return map();
            case REDUCE:
                return reduce();
            case SUB:
                consume();
                final Token tok = LA(0);
                switch (tok.getKind()) {
                    case INT:
                        return intLiteral(firstTok);
                    case FLOAT:
                        return floatLiteral(firstTok);
                    default:
                        throw new SynaxError(firstTok, "unexpected token: " + tok.getText() + " expected integer or float");
                }
            default:
                throw new SynaxError(firstTok, "unexpected token: " + firstTok.getText() + " expected expression");
        }
    }

    private boolean isOperation(Token tok) {
        if (!isEOF(tok)) {
            switch (tok.getKind()) {
                case ADD:
                case SUB:
                case DIV:
                case MUL:
                case POW:
                    return true;
            }
        }
        return false;
    }

    private BinaryOpExpr.OpKind getOpKind(Token tok) {
        switch (tok.getKind()) {
            case ADD:
                return BinaryOpExpr.OpKind.ADD;
            case SUB:
                return BinaryOpExpr.OpKind.SUB;
            case DIV:
                return BinaryOpExpr.OpKind.DIV;
            case MUL:
                return BinaryOpExpr.OpKind.MUL;
            case POW:
                return BinaryOpExpr.OpKind.POW;
            default:
                assert false : "should be an operation: " + tok;
                return null;
        }
    }

    private IntLiteral intLiteral() throws SynaxError {
        return intLiteral(null);
    }

    private IntLiteral intLiteral(Token signTok) throws SynaxError {
        final Token tok = LA(0);
        consume();
        assert tok.getKind() == Token.Kind.INT;
        try {
            int value = Integer.parseInt(tok.getText().toString());
            if (signTok == null) {
            } else {
            }
            if (signTok == null) {
                return new IntLiteral(tok.getLine(), tok.getColumn(), tok.getText(), value);
            } else {
                return new IntLiteral(signTok.getLine(), signTok.getColumn(), negationText(tok, signTok), -value);
            }
        } catch (NumberFormatException e) {
            // e.getMessage() returns meaningless "for XXXXXX" where XXXXXX is the text being parsed
            // lexer already checked syntax, but not the max/min =>
            throw new SynaxError(tok, "integer out of limits");
        }
    }

    private FloatLiteral floatLiteral() throws SynaxError {
        return floatLiteral(null);
    }

    private FloatLiteral floatLiteral(Token signTok) throws SynaxError {
        final Token tok = LA(0);
        consume();
        assert tok.getKind() == Token.Kind.FLOAT;
        try {
            double value = Double.parseDouble(tok.getText().toString());
            if (signTok == null) {
                return new FloatLiteral(tok.getLine(), tok.getColumn(), tok.getText(), value);
            } else {
                return new FloatLiteral(signTok.getLine(), signTok.getColumn(), negationText(tok, signTok), -value);
            }
        } catch (NumberFormatException e) {
            // e.getMessage() returns meaningless "for XXXXXX" where XXXXXX is the text being parsed
            // lexer already checked syntax, but not the max/min =>
            throw new SynaxError(tok, "float number or its precision out of limits");
        }
    }

    private CharSequence negationText(Token tok, Token signTok) {
        if (signTok.getLine() == tok.getLine()) {
            int spaces = tok.getColumn()  - signTok.getColumn() - 1;
            StringBuilder sb = new StringBuilder(signTok.getText());
            while (spaces-- > 0) {
                sb.append(' ');
            }
            sb.append(tok.getText());
            return sb;
        } else {
            return "" + signTok.getText() + tok.getText();
        }
    }

    private StringLiteral stringLiteral() throws SynaxError {
        final Token tok = LA(0);
        consume();
        assert tok.getKind() == Token.Kind.STRING;
        return new StringLiteral(tok.getLine(), tok.getColumn(), tok.getText());
    }

    private IdExpr id() throws SynaxError {
        final Token tok = LA(0);
        assert tok.getKind() == Token.Kind.ID;
        consume();
        CharSequence name = tok.getText();
        Type type = symtab.getType(name);
        if (type == null) {
            error(tok, "undeclared variable " + name);
            type = Type.ERRONEOUS;
        }
        return new IdExpr(name, tok.getLine(), tok.getColumn(), type);
    }

    private ParenExpr paren() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.LPAREN);
        Expr expr = expression();
        consumeExpected(Token.Kind.RPAREN);
        return new ParenExpr(firstTok.getLine(), firstTok.getColumn(), expr);
    }

    private SeqExpr seq() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.LCURLY);
        Expr first = expression();
        consumeExpected(Token.Kind.COMMA);
        Expr last = expression();
        checkSeqBoundExprType(first);
        checkSeqBoundExprType(last);
        consumeExpected(Token.Kind.RCURLY);
        return new SeqExpr(firstTok.getLine(), firstTok.getColumn(), first, last);
    }
    
    private void checkSeqBoundExprType(Expr expr) {
        if (expr.getType() != Type.INT && expr.getType() != Type.ERRONEOUS) {
            error(expr, "seqence bound is not an integer expression");
        }
    }

    /** returns type of an element in sequence */
    private Type seqElementType(Expr seq, boolean reportErrors) {
        Type seqType = seq.getType();
        switch (seqType) {
            case SEQ_INT:
                return Type.INT;
            case SEQ_FLOAT:
                return Type.FLOAT;
            case INT:
            case FLOAT:
            case STRING:
                if (reportErrors) {
                    error(seq, "wrong type: " + seqType.getDisplayName() + ", expected sequence");
                }
                return Type.ERRONEOUS;
            case ERRONEOUS:
                return Type.ERRONEOUS;
            default:
                throw new AssertionError(seqType.name());
        }
    }

    private MapExpr map() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.MAP);
        consumeExpected(Token.Kind.LPAREN);
        Expr seq = expression();
        pushSymtab(false);
        try {
            Type varType = seqElementType(seq, true);
            consumeExpected(Token.Kind.COMMA);
            DeclStatement var = lambdaVarDecl(varType);
            consumeExpected(Token.Kind.ARROW);
            symtab.put(var.getDelarationName(), Type.INT);
            Expr transformation = expression();
            consumeExpected(Token.Kind.RPAREN);
            return new MapExpr(firstTok.getLine(), firstTok.getColumn(), seq, var, transformation);
        } finally {
            popSymtab();
        }
    }

    private ReduceExpr reduce() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.REDUCE);
        consumeExpected(Token.Kind.LPAREN);
        Expr seq = expression();
        Type varType = seqElementType(seq, true);
        consumeExpected(Token.Kind.COMMA);
        Expr defValue = expression();
        consumeExpected(Token.Kind.COMMA);
        pushSymtab(false);
        try {       
            DeclStatement prev = lambdaVarDecl(varType);
            DeclStatement curr = lambdaVarDecl(varType);
            consumeExpected(Token.Kind.ARROW);
            symtab.put(prev.getDelarationName(), Type.INT);
            symtab.put(curr.getDelarationName(), Type.INT);
            Expr transformation = expression();
            consumeExpected(Token.Kind.RPAREN);
            return new ReduceExpr(firstTok.getLine(), firstTok.getColumn(), seq, defValue, prev, curr, transformation);
        } finally {
            popSymtab();
        }
    }

    /**
     * Checks that LA(0) is of the given kind and consumes it; otherwise throws SynaxError
     * @return consumed token
     */
    private Token consumeExpected(Token.Kind kind) throws SynaxError {
        Token tok = LA(0);
        if (isEOF(tok) || tok.getKind() != kind) {
            throw new SynaxError(tok, "expected " + getTokenKindName(kind));
        }
        consume();
        return tok;
    }

    private String getTokenKindName(Token.Kind kind) {
        if (kind.isFixedText()) {
            return kind.getFixedText();
        }
        switch (kind) {
            case INT:
                return "integer";
            case FLOAT:
                return "float";
            case STRING:
                return "string";
            case ID:
                return "identifier";
            default:
                assert false : "unexpected token kind: " + kind;
                return kind.toString();
        }
    }

    /** just a conveniency shortcut */
    private Token LA(int lookAhead)  {
        try {
            return tokens.LA(lookAhead);
        } catch (TokenStreamException ex) {
            diagnosticListener.report(Diagnostic.fatal(-1, -1, ex.getMessage()));
        }
        return null; // TODO: return EOF or throw an exception
    }

    /** just a conveniency shortcut */
    private void consume() {
        tokens.consume();
    }

    private boolean isEOF(Token tok) {
        return Token.isEOF(tok);
    }
    private boolean isEOF() {
        try {
            return isEOF(tokens.LA(0));
        } catch (TokenStreamException ex) {
            return false;
        }
    }

    private static Diagnostic toDiagnostic(SynaxError err) {
        return Diagnostic.error(err.getLine(), err.getColumn(), err.getLocalizedMessage());
    }
    
    private void error(ASTNode node, CharSequence message) {
        diagnosticListener.report(Diagnostic.error(node.getLine(), node.getColumn(), message));
    }

    private void error(Token tok, CharSequence message) {
        diagnosticListener.report(Diagnostic.error(tok.getLine(), tok.getColumn(), message));
    }

    private void error(int line, int column, CharSequence message) {
        diagnosticListener.report(Diagnostic.error(line, column, message));
    }

    private static class SynaxError extends Exception {
        
        private final int line;
        private final int column;
        
        SynaxError(Token tok, String message) {
            super(message);
            if (tok == null/*paranoia*/) {
                line = column = Integer.MAX_VALUE;
            } else {
                line = tok.getLine();
                column = tok.getColumn();
            }
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }
    }
    
    private void pushSymtab(boolean transitive) {
        symtab =  new Symtab(symtab, transitive);
    }
    
    private void popSymtab() {
        assert symtab.previous != null;
        symtab = symtab.previous;
    }

    private static class Symtab {

        private boolean transitive;
        private Symtab previous;
        private Map<CharSequence, Type> data = new TreeMap<>();

        public Symtab(Symtab previous, boolean transitive) {
            this.transitive = transitive;
            this.previous = previous;
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
        
        public Type getType(CharSequence name) {
            Type type = data.get(name);
            if (type == null && transitive && previous != null) {
                type = previous.getType(name);
            }
            return type;
        }

        public void put(CharSequence name, Type type) {
            data.put(name, type);
        }
    }

    public class CombinedDiagnosticListener implements DiagnosticListener {
        private final DiagnosticListener[] listeners;
        public CombinedDiagnosticListener(DiagnosticListener[] listeners) {
            this.listeners = listeners;
        }
        @Override
        public void report(Diagnostic issue) {
            for (DiagnosticListener l : listeners) {
                l.report(issue);
            }
        }
    }
}
