package org.jb.lexer.api;

import java.io.InputStream;
import org.jb.lexer.impl.LexerImpl;

/**
 * Lexes the given input stream and returns token sequence.
 * @author vkvashin
 */
public final class Lexer {
    
    private final LexerImpl lexer;
    
    public Lexer(InputStream is) {
        lexer = new LexerImpl(is);
    }
    
    /** Lex the input stream and return token sequence. */
    public TokenStream lex() {
        return lexer.lex();
    }
}


