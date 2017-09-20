package org.jb.parser.api;

import org.jb.ast.api.JbNode;
import java.util.Collections;
import java.util.List;
import org.jb.lexer.api.JbTokenStream;
import org.jb.parser.impl.ParserImpl;

/**
 *
 * @author vkvashin
 */
public final class JbParser {
    
    public JbNode parse(JbTokenStream ts) {
        return new ParserImpl().parse(ts);
    }
}
