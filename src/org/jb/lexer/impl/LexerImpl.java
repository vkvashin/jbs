package org.jb.lexer.impl;

import java.io.IOException;
import java.io.InputStream;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.ast.diagnostics.DiagnosticListener;
import org.jb.lexer.api.Token;
import org.jb.lexer.api.TokenFactory;
import org.jb.lexer.api.TokenStreamException;
import org.jb.lexer.api.TokenStream;

/**
 * Lexer implementation
 * @author vkvashin
 */
public class LexerImpl {
    
    private final InputStreamWrapper is;
    private final TokenFactory tokenFactory;
    private final DiagnosticListener diagnosticListener;

    public LexerImpl(InputStream is, TokenFactory tokenFactory, DiagnosticListener diagnosticListener) {
        this.is = new InputStreamWrapper(is);
        this.tokenFactory = tokenFactory;
        this.diagnosticListener = diagnosticListener;
    }

    public TokenStream lex() {
        return new TokenStreamImpl();
    }
    
    /** Wraps input stream; maintains line and column; also allows to unread 1 char */
    private class InputStreamWrapper {

        private final InputStream is;
        private int line = 1; 
        private int column = 0; 
        private char unread = 0;

        public InputStreamWrapper(InputStream is) {
            this.is = is;
        }
        
        public void unread(char c) {
            assert unread == 0 : "attempt to unread 2 times";
            unread = c;
            --column;
        }
        
        /** @return next char or '\0'; the latter means EOF  */
        public char read() throws IOException {            
            if (unread != 0) {
                char c = unread;
                unread = 0;
                ++column;
                return c;
            } else {
                unread = 0;                
            }

            int read = is.read();
            ++column;
            if (read == -1) {
                return 0;
            }
            char c = (char) read;
            if (c == '\n') {
                ++line;
                column = 0; // next char will set it to 1
            }
            return c;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }
    }
    
    @FunctionalInterface
    private interface BoolPredicate {
        boolean isTrue(char c);
    }
    
    private class TokenStreamImpl implements TokenStream {
        
        private Token EOF = null;

        @Override
        public Token next() throws TokenStreamException {            
            try {
                while (true) {
                    Token tok = nextImpl();
                    if (tok != null) {
                        return tok;
                    }
                    consumeWhile((char curr) -> !isSpace(curr)); // consume until next space
                }
            } catch (IOException ex) {
                throw new TokenStreamException(ex);
            }
        }
        
        private Token nextImpl() throws IOException, TokenStreamException {            
            char c;
            // skip whitespaces
            while (true) {
                c = is.read();
                if (c == 0) {
                    return (EOF == null) ? (EOF = tokenFactory.createFixed(Token.Kind.EOF, is.getLine(), is.getColumn())) : EOF;
                }
                if (isSpace(c)) {
                    continue;
                } else {
                    break;
                }
            }
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    return readNumber(c);
                case '-':
                    return readArrowOpOrNumber(c);
                case '+':
                case '*':
                case '/':
                case '^':
                    return readOp(c);
                case '"':
                    return readString();
                case '(':
                    return tokenFactory.createFixed(Token.Kind.LPAREN, is.getLine(), is.getColumn());
                case ')':
                    return tokenFactory.createFixed(Token.Kind.RPAREN, is.getLine(), is.getColumn());
                case '{':
                    return tokenFactory.createFixed(Token.Kind.LCURLY, is.getLine(), is.getColumn());
                case '}':
                    return tokenFactory.createFixed(Token.Kind.RCURLY, is.getLine(), is.getColumn());
                case ',':
                    return tokenFactory.createFixed(Token.Kind.COMMA, is.getLine(), is.getColumn());
                case '=':
                    return tokenFactory.createFixed(Token.Kind.EQ, is.getLine(), is.getColumn());
                default:
                    if (Character.isJavaIdentifierStart(c)) {
                        return readIdVarOrFunction(c);
                    } else {
                        diagnosticListener.report(Diagnostic.error(is.getLine(), is.getColumn(), "unexpected character '" + c + "'"));
                        return null;
                    }                                        
            }
        }
        
        private void consumeWhile(BoolPredicate condition) throws IOException {
            char c;
            do {
                c = is.read();
            } while (c != 0 && condition.isTrue(c));
        }

        private Token readNumber(char c) throws IOException, TokenStreamException {
            int line = is.getLine();
            int col = is.getColumn();
            StringBuilder sb = new StringBuilder().append(c);
            Token.Kind kind = Token.Kind.INT;
            while (true) {
                c = is.read();
                if (Character.isDigit(c)) {
                    sb.append(c);
                } else if(c == '.') {
                    if (kind == Token.Kind.INT) {
                        kind = Token.Kind.FLOAT;
                        sb.append(c);
                    } else {
                        diagnosticListener.report(Diagnostic.error(line, col, "more than one digital point"));
                        consumeWhile((char curr) -> curr == '.' || Character.isDigit(curr));
                        break;
                    }
                } else {
                    is.unread(c);
                    break;
                }
            }            
            return tokenFactory.create(kind, sb, line, col);
        }

        private Token readString() throws IOException, TokenStreamException {
            int line = is.getLine();
            int col = is.getColumn();
            StringBuilder sb = new StringBuilder();
            for(char c = is.read(); c != '"'; c = is.read()) {
                if (c == 0) {
                    diagnosticListener.report(Diagnostic.error(is.getLine(), is.getColumn(), "unterminated string"));
                    break;
                }
                sb.append(c);
            }
            return tokenFactory.create(Token.Kind.STRING, sb, line, col);
        }

        private Token readIdVarOrFunction(char c) throws TokenStreamException, IOException {
            assert Character.isJavaIdentifierStart(c);
            StringBuilder sb = new StringBuilder().append(c);
            int line = is.getLine();
            int col = is.getColumn();
            for(c = is.read(); c != 0 && Character.isJavaIdentifierPart(c); c = is.read()) {
                sb.append(c);
            }
            is.unread(c);
            for (Token.Kind kind : new Token.Kind[] {Token.Kind.MAP, Token.Kind.REDUCE, Token.Kind.PRINT, Token.Kind.OUT, Token.Kind.VAR}) {
                assert kind.isFixedText();
                if (kind.getFixedText().contentEquals(sb)) {
                    return tokenFactory.createFixed(kind, line, col);
                }
            }
            return tokenFactory.create(Token.Kind.ID, sb, line, col);
        }

        private Token readOp(char c) {
            switch (c) {
                case '+':   return tokenFactory.createFixed(Token.Kind.ADD, is.getLine(), is.getColumn());
                case '-':   return tokenFactory.createFixed(Token.Kind.SUB, is.getLine(), is.getColumn());
                case '*':   return tokenFactory.createFixed(Token.Kind.MUL, is.getLine(), is.getColumn());
                case '/':   return tokenFactory.createFixed(Token.Kind.DIV, is.getLine(), is.getColumn());
                case '^':   return tokenFactory.createFixed(Token.Kind.POW, is.getLine(), is.getColumn());
                default:    throw new IllegalArgumentException("Unexpected readOp('" + c + "')");
            }
        }

        private Token readArrowOpOrNumber(char c) throws IOException, TokenStreamException {
            assert c == '-';
            c = is.read();
            if (c == 0) {
                return tokenFactory.createFixed(Token.Kind.SUB, is.getLine(), is.getColumn());
            } else if (c == '>') {
                return tokenFactory.createFixed(Token.Kind.ARROW, is.getLine(), is.getColumn() - 1);
            } else if(Character.isDigit(c)) {
                is.unread(c);
                return readNumber('-');
            } else {
                is.unread(c);
                return tokenFactory.createFixed(Token.Kind.SUB, is.getLine(), is.getColumn());
            }            
        }
    }
    
    private static boolean isSpace(char c) {
        return c == '\n' || c == '\r' || Character.isSpaceChar(c);
    }
}
