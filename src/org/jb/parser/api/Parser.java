package org.jb.parser.api;

import org.jb.ast.diagnostics.DefaultDiagnosticListener;
import org.jb.ast.api.ASTNode;
import java.util.Collections;
import java.util.List;
import org.jb.lexer.api.TokenStreamException;
import org.jb.parser.impl.ParserImpl;
import org.jb.lexer.api.TokenStream;
import org.jb.ast.diagnostics.DiagnosticListener;

/**
 * Parses token stream and builds AST
 * @author vkvashin
 */
public final class Parser {
    
    public ASTNode parse(TokenStream ts) throws TokenStreamException {
        return parse(ts, DefaultDiagnosticListener.getDefaultListener());
    }

    public ASTNode parse(TokenStream ts, DiagnosticListener... errorListeners) throws TokenStreamException {
        return new ParserImpl(ts, errorListeners).parse();
    }
}
