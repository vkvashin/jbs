package org.jb.parser.impl;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jb.ast.api.DeclStatement;
import org.jb.ast.api.ASTNode;
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
        tokens = new WindowTokenBuffer(ts, 1, 4096);
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
                throw new SynaxError(tok, "statement should start with var, print or out");
        }
    }

    private DeclStatement declStatement() throws SynaxError {
        final Token firstTok = LA(0);
        assert firstTok.getKind() == Token.Kind.VAR;
        consume(); // var
        final Token nameTok = LA(0);
        if (nameTok == null || nameTok.getKind() != Token.Kind.ID) {
            throw new SynaxError(LA(0), "var keyword should be followed by name");
        }
        consume(); // variable name
        if (LA(0).getKind() != Token.Kind.EQ) {
            throw new SynaxError(LA(0), "variable name should be followed by = sign");
        }
        consume();// =
        Expr expr = expression();
        return new DeclStatement(firstTok.getLine(), firstTok.getColumn(), nameTok.getText(), expr);
    }

    private PrintStatement printStatement() throws SynaxError {
        final Token firstTok = LA(0);
        assert firstTok.getKind() == Token.Kind.PRINT;
        consume(); // print
        final Token stringTok = LA(0);
        consume(); // string
        if (stringTok.getKind() != Token.Kind.STRING) {
            throw new SynaxError(LA(0), "print keyword should be followed by a string");
        }
        StringLiteral sl = new StringLiteral(stringTok.getLine(), stringTok.getColumn(), stringTok.getText());
        return new PrintStatement(firstTok.getLine(), firstTok.getColumn(), sl);
    }

    private OutStatement outStatement() throws SynaxError {
        final Token firstTok = LA(0);
        assert firstTok.getKind() == Token.Kind.OUT;
        consume(); // out
        Expr expr = expression();
        return new OutStatement(firstTok.getLine(), firstTok.getColumn(), expr);
    }

    private Expr expression() throws SynaxError {
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
                throw new SynaxError(firstTok, "unexpected token: expected expression");
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
        //throw new UnsupportedOperationException("not supported yet");
        return null;
    }

    private ParenExpr paren() throws SynaxError {
        //throw new UnsupportedOperationException("not supported yet");
        return null;
    }

    private SeqExpr seq() throws SynaxError {
        //throw new UnsupportedOperationException("not supported yet");
        return null;
    }

    private MapExpr map() throws SynaxError {
        //throw new UnsupportedOperationException("not supported yet");
        return null;
    }

    private ReduceExpr reduce() throws SynaxError {
        //throw new UnsupportedOperationException("not supported yet");
        return null;
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
