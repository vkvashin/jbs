package org.jb.parser.api;

import org.jb.ast.api.JbNode;
import java.util.Collections;
import java.util.List;
import org.jb.lexer.api.JbTokenStream;
import org.jb.lexer.api.JbTokenStreamException;
import org.jb.parser.impl.ParserImpl;

/**
 *
 * @author vkvashin
 */
public final class Parser {
    
    public JbNode parse(JbTokenStream ts) throws JbTokenStreamException {
        return parse(ts, new DefaultParseErrorListener());
    }

    public JbNode parse(JbTokenStream ts, ParseErrorListener errorListener) throws JbTokenStreamException {
        return new ParserImpl(ts, errorListener).parse();
    }
}
