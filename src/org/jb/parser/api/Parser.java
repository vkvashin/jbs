package org.jb.parser.api;

import org.jb.ast.api.ASTNode;
import java.util.Collections;
import java.util.List;
import org.jb.lexer.api.TokenStreamException;
import org.jb.parser.impl.ParserImpl;
import org.jb.lexer.api.TokenStream;

/**
 *
 * @author vkvashin
 */
public final class Parser {
    
    public ASTNode parse(TokenStream ts) throws TokenStreamException {
        return parse(ts, new DefaultParseErrorListener());
    }

    public ASTNode parse(TokenStream ts, ParseErrorListener errorListener) throws TokenStreamException {
        return new ParserImpl(ts, errorListener).parse();
    }
}
