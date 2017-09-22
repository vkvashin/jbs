package org.jb.lexer.api;

import java.util.Objects;

/**
 * Represents a token.
 * That's a quite straightforward token implementation, without any optimizations
 * Possible optimizations are:
 *  - subclassing for fixed text tokens to get rid of this.text
 *  - subclassing for tokens with small line & col that can be but into a single int
 *  - using smart CharSequence
 * @author vkvashin
 */
public final class Token {
    
    public enum Kind {
        INT,    // integer literal
        FLOAT,  // float literal
        STRING, // literal string
        ID,     // identifier
        VAR("var"),
        ADD("+"),
        SUB("-"),
        DIV("/"),
        MUL("*"),
        POW("^"),
        LPAREN("("),
        RPAREN(")"),
        LCURLY("{"),
        RCURLY("}"),
        COMMA(","),
        ARROW("->"),
        MAP("map"),
        REDUCE("reduce"),
        EQ("="),
        PRINT("print"),
        OUT("out"),
        EOF("EOF");

        final String fixedText;

        private Kind() {
            this.fixedText = null;
        }

        private Kind(String fixedText) {
            this.fixedText = fixedText;
        }
        
        public boolean isFixedText() {
            return fixedText != null;
        }

        public String getFixedText() {
            return fixedText;
        }
    }

    private final Kind kind;
    private final CharSequence text;
    private final int line;
    private final int column;

    /** 
     * Package-level ctor: it's not a clients' business to create token instances -
     * see TokenFactory and TokenFactoryImpl.
     */
    /*package*/ Token(Kind kind, CharSequence text, int line, int column) {
        this.kind = kind;
        this.text = text;
        this.line = line;
        this.column = column;
    }

    public Kind getKind() {
        return kind;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
    
    public CharSequence getText() {
        return text;
    }    
    
    public boolean isEOF() {
        return getKind() == Kind.EOF;
    }
    
    public static boolean isEOF(Token token) {
        return (token == null/*paranoia*/) || token.getKind() == Kind.EOF;
    }

    @Override
    public String toString() {
        return kind.toString() + ' ' + text + " [" + line + ':' + column + "] ";
    }    

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.kind);
        hash = 79 * hash + Objects.hashCode(this.text);
        hash = 79 * hash + this.line;
        hash = 79 * hash + this.column;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Token other = (Token) obj;
        if (this.line != other.line) {
            return false;
        }
        if (this.column != other.column) {
            return false;
        }
        if (this.kind != other.kind) {
            return false;
        }
        if (!Objects.equals(this.text, other.text)) {
            return false;
        }
        return true;
    }    
}
