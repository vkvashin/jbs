package org.jb.lexer.api;

import java.io.InputStream;
import org.jb.lexer.impl.LexerImpl;

/**
 * Lexes the given input stream and returns token sequence.
 * @author vkvashin
 */
public final class JbLexer {
    
    private final LexerImpl lexer;
    
    public JbLexer(InputStream is) {
        lexer = new LexerImpl(is);
    }
    
    /** Lex the input stream and return token sequence. */
    public JbTokenStream lex() {
        return lexer.lex();
    }
}


