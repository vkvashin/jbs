package org.jb.lexer.api;

import java.io.InputStream;
import org.jb.ast.diagnostics.DefaultDiagnosticListener;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.ast.diagnostics.DiagnosticListener;
import org.jb.lexer.impl.LexerImpl;

/**
 * Lexes the given input stream and returns token sequence.
 * @author vkvashin
 */
public final class Lexer {
    
    private final LexerImpl lexer;
    
    public Lexer(InputStream is) {
        this(is, new DefaultDiagnosticListener());
    }

    public Lexer(InputStream is, DiagnosticListener diagnosticListener) {
        lexer = new LexerImpl(is, new TokenFactoryImpl(), diagnosticListener);
    }

    /** Lex the input stream and return token sequence. */
    public TokenStream lex() {
        return lexer.lex();
    }
}


