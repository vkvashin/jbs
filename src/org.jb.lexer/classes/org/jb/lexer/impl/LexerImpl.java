package org.jb.lexer.impl;

import java.io.IOException;
import java.io.InputStream;
import org.jb.lexer.api.JbToken;
import org.jb.lexer.api.JbTokenStream;
import org.jb.lexer.api.JbTokenStreamException;

/**
 * Lexer implementation
 * @author vkvashin
 */
public class LexerImpl {
    
    private final InputStreamWrapper is;

    public LexerImpl(InputStream is) {
        this.is = new InputStreamWrapper(is);
    }

    public JbTokenStream lex() {
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
    
    private class TokenStreamImpl implements JbTokenStream {
        

        @Override
        public JbToken next() throws JbTokenStreamException {            
            try {
                return nextImpl();
            } catch (IOException ex) {
                throw new JbTokenStreamException(ex);
            }
        }
        
        private JbToken nextImpl() throws IOException, JbTokenStreamException {            
            char c;
            // skip whitespaces
            while (true) {
                c = is.read();
                if (c == 0) {
                    return null;
                }
                if (Character.isSpaceChar(c)) {
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
                    return readNumberOrOp(c);
                case '*':
                case '/':
                case '^':
                    return readOp(c);
                case '"':
                    return readString();
                case '(':
                    return JbToken.createFixed(JbToken.Kind.LPAREN, is.getLine(), is.getColumn());
                case ')':
                    return JbToken.createFixed(JbToken.Kind.RPAREN, is.getLine(), is.getColumn());
                case '{':
                    return JbToken.createFixed(JbToken.Kind.LCURLY, is.getLine(), is.getColumn());
                case '}':
                    return JbToken.createFixed(JbToken.Kind.RCURLY, is.getLine(), is.getColumn());
                case ',':
                    return JbToken.createFixed(JbToken.Kind.COMMA, is.getLine(), is.getColumn());
                case '=':
                    return JbToken.createFixed(JbToken.Kind.EQ, is.getLine(), is.getColumn());
                default:
                    return readIdOrFunction(c);
            }
        }

        private JbToken readNumber(char c) throws IOException, JbTokenStreamException {
            int line = is.getLine();
            int col = is.getColumn();
            StringBuilder sb = new StringBuilder().append(c);
            JbToken.Kind kind = JbToken.Kind.INT;
            while (true) {
                c = is.read();
                if (Character.isDigit(c)) {
                    sb.append(c);
                } else if(c == '.') {
                    if (kind == JbToken.Kind.INT) {
                        kind = JbToken.Kind.FLOAT;
                        sb.append(c);
                    } else {
                        throw new JbTokenStreamException("Syntax error: two digital points at " + is.getLine() + ':' + is.getColumn());
                    }
                } else {
                    is.unread(c);
                    break;
                }
            }            
            return JbToken.create(kind, sb, line, col);
        }

        private JbToken readString() throws IOException, JbTokenStreamException {
            int line = is.getLine();
            int col = is.getColumn();
            StringBuilder sb = new StringBuilder();
            for(char c = is.read(); c != '"'; c = is.read()) {
                if (c == 0) {
                    throw new JbTokenStreamException("Syntax error: unterminated string at " + is.getLine() + ':' + is.getColumn());
                }
                sb.append(c);
            }
            return JbToken.create(JbToken.Kind.STRING, sb, line, col);
        }

        private JbToken readIdOrFunction(char c) throws JbTokenStreamException, IOException {
            if (!Character.isJavaIdentifierStart(c)) {
                throw new JbTokenStreamException("Syntax error: unexpected character '" + c + "' at " + is.getLine() + ':' + is.getColumn());
            }
            StringBuilder sb = new StringBuilder().append(c);
            int line = is.getLine();
            int col = is.getColumn();
            for(c = is.read(); c != 0 && Character.isJavaIdentifierPart(c); c = is.read()) {
                sb.append(c);
            }
            is.unread(c);
            for (JbToken.Kind kind : new JbToken.Kind[] {JbToken.Kind.MAP, JbToken.Kind.REDUCE, JbToken.Kind.PRINT, JbToken.Kind.OUT}) {
                assert kind.isFixedText();
                if (kind.getFixedText().contentEquals(sb)) {
                    return JbToken.createFixed(kind, line, col);
                }
            }
            return JbToken.create(JbToken.Kind.ID, sb, line, col);
        }

        private JbToken readNumberOrOp(char c) throws IOException, JbTokenStreamException {
            assert c == '+';
            c = is.read();
            if (c == 0) {
                return JbToken.createFixed(JbToken.Kind.ADD, is.getLine(), is.getColumn());
            } else if(Character.isDigit(c)) {
                is.unread(c);
                return readNumber('+');
            } else {
                is.unread(c);
                return JbToken.createFixed(JbToken.Kind.ADD, is.getLine(), is.getColumn());
            }            
        }

        private JbToken readOp(char c) {
            switch (c) {
                case '+':   return JbToken.createFixed(JbToken.Kind.ADD, is.getLine(), is.getColumn());
                case '-':   return JbToken.createFixed(JbToken.Kind.SUB, is.getLine(), is.getColumn());
                case '*':   return JbToken.createFixed(JbToken.Kind.MUL, is.getLine(), is.getColumn());
                case '/':   return JbToken.createFixed(JbToken.Kind.DIV, is.getLine(), is.getColumn());
                case '^':   return JbToken.createFixed(JbToken.Kind.POW, is.getLine(), is.getColumn());
                default:    throw new IllegalArgumentException("Unexpected readOp('" + c + "')");
            }
        }

        private JbToken readArrowOpOrNumber(char c) throws IOException, JbTokenStreamException {
            assert c == '-';
            c = is.read();
            if (c == 0) {
                return JbToken.createFixed(JbToken.Kind.SUB, is.getLine(), is.getColumn());
            } else if (c == '>') {
                return JbToken.createFixed(JbToken.Kind.ARROW, is.getLine(), is.getColumn() - 1);
            } else if(Character.isDigit(c)) {
                is.unread(c);
                return readNumber('-');
            } else {
                is.unread(c);
                return JbToken.createFixed(JbToken.Kind.SUB, is.getLine(), is.getColumn());
            }            
        }
    }    
}
