package org.jb.parser.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Stack;
import org.jb.ast.api.DeclStatement;
import org.jb.ast.api.*;
import org.jb.lexer.api.*;
import org.jb.parser.api.*;

/**
 *
 * @author vkvashin
 */
public class ParserImpl {

    private final TokenBuffer tokens;
    private final ParseErrorListener errorListener;

    public ParserImpl(TokenStream ts, ParseErrorListener errorListener) throws TokenStreamException {
        tokens = new ZeroLookaheadTokenBuffer(ts); //WindowTokenBuffer(ts, 1, 4096);
        this.errorListener = errorListener;
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
        if (tok != null) {
            int line = tok.getLine();
            while (tok != null && tok.getLine() == line) {
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
                errorListener.error(ex);
                skipToTheNextLine();
            }
        }
    }

    private Statement statementImpl() throws SynaxError {
        Token tok = LA(0);
        if (tok == null) {
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
        if (nameTok == null || nameTok.getKind() != Token.Kind.ID) {
            throw new SynaxError(LA(0), "var keyword should be followed by name");
        }
        if (LA(0).getKind() != Token.Kind.EQ) {
            throw new SynaxError(LA(0), "variable name should be followed by = sign");
        }
        consume();// =
        Expr expr = expression();
        return new DeclStatement(firstTok.getLine(), firstTok.getColumn(), nameTok.getText(), expr);
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
            if (stackOp != null && stackOp.isStronger(currOp)) {
                outQueue.add(opStack.pop());
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
        return new BinaryOpExpr(line, column, op, left, right);
    }

    private Expr operand() throws SynaxError {
        final Token firstTok = LA(0);
        if (firstTok == null) {
            throw new SynaxError("unexpected end of file: expected expression");
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
            default:
                throw new SynaxError(firstTok, "unexpected token: " + firstTok.getText() + " expected expression");
        }
    }

    private boolean isOperation(Token tok) {
        if (tok != null) {
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
        final Token tok = LA(0);
        consume();
        assert tok.getKind() == Token.Kind.INT;
        return new IntLiteral(tok.getLine(), tok.getColumn(), tok.getText());
    }

    private FloatLiteral floatLiteral() throws SynaxError {
        final Token tok = LA(0);
        consume();
        assert tok.getKind() == Token.Kind.FLOAT;
        return new FloatLiteral(tok.getLine(), tok.getColumn(), tok.getText());
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
        return new IdExpr(tok.getText(), tok.getLine(), tok.getColumn());
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
        consumeExpected(Token.Kind.RCURLY);
        return new SeqExpr(firstTok.getLine(), firstTok.getColumn(), first, last);
    }

    private MapExpr map() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.MAP);
        consumeExpected(Token.Kind.LPAREN);
        Expr sequence = expression();
        consumeExpected(Token.Kind.COMMA);
        IdExpr var = id();
        consumeExpected(Token.Kind.ARROW);
        Expr transformation = expression();
        consumeExpected(Token.Kind.RPAREN);
        return new MapExpr(firstTok.getLine(), firstTok.getColumn(), sequence, var, transformation);
    }

    private ReduceExpr reduce() throws SynaxError {
        final Token firstTok = consumeExpected(Token.Kind.REDUCE);
        consumeExpected(Token.Kind.LPAREN);
        Expr sequence = expression();
        consumeExpected(Token.Kind.COMMA);
        Expr defValue = expression();
        consumeExpected(Token.Kind.COMMA);
        IdExpr prev = id();
        IdExpr curr = id();
        consumeExpected(Token.Kind.ARROW);
        Expr transformation = expression();
        consumeExpected(Token.Kind.RPAREN);
        return new ReduceExpr(firstTok.getLine(), firstTok.getColumn(), sequence, defValue, prev, curr, transformation);
    }

    /**
     * Checks that LA(0) is of the given kind and consumes it; otherwise throws SynaxError
     * @return consumed token
     */
    private Token consumeExpected(Token.Kind kind) throws SynaxError {
        Token tok = LA(0);
        if (tok == null || tok.getKind() != kind) {
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
    private Token LA(int lookAhead) {
        // skip some erroneous tokens
        for (int i = 0; i < 10; i++) {
            try {
                return tokens.LA(lookAhead);
            } catch (TokenStreamException ex) {
                tokens.consume();
                errorListener.error(ex);
            }
        }
        return null;
    }

    /** just a conveniency shortcut */
    private void consume() {
        tokens.consume();
    }

    private boolean isEOF() {
        try {
            return tokens.LA(0) == null;
        } catch (TokenStreamException ex) {
            return false;
        }
    }

    private static class SynaxError extends Exception {
        SynaxError(String message) {
            this(null, message);
        }
        SynaxError(Token tok, String message) {
            super(composeMessage(tok, message));
        }
        private static String composeMessage(Token tok, String message) {
            if (tok == null) {
                return "Syntax error: " + message;
            } else {
                return "Syntax error in " + tok.getLine() + ':' + tok.getColumn() + ": " + message;
            }
        }
    }
}
