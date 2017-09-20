/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.api;

import java.util.ArrayList;
import org.jb.ast.api.JbNode;
import org.jb.lexer.api.JbToken;
import org.jb.lexer.api.JbTokenStream;
import org.jb.lexer.api.JbTokenStreamException;
import org.jb.parser.impl.TokenBuffer;
import org.jb.parser.impl.ArrayTokenBuffer;
import org.jb.parser.impl.WindowTokenBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author vkvashin
 */
public class ParserSimpleTest extends ParserTestBase {

    public ParserSimpleTest() {
    }

    @FunctionalInterface
    private interface TokenBufferFactory {
        TokenBuffer create(JbTokenStream ts, int maxLA) throws JbTokenStreamException;
    }

    public void doTestTokenBuffer(TokenBufferFactory factory, int maxLA) throws Exception {
        String text = "\n" +
            "var n = 500\n" +
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" +
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "print \"pi = \"\n" +
            "out pi";
        
        JbToken[] refTokens = lexAndGetTokenArray(text);
        TokenBuffer tb = factory.create(lex(text), maxLA);
        for (int base = 0; base < refTokens.length; base++) {
            for(int la = 0; la < maxLA; la++) {
                int idx = base + la;
                if (idx < refTokens.length) {
                    JbToken tok = tb.LA(la);
                    if (tok == null) {
                        tok = tb.LA(la);
                    }
                    assertEquals(refTokens[idx], tok);
                }
            }
            tb.consume();
        }
    }

    @Test
    public void testArrayTokenBuffer() throws Exception {
        doTestTokenBuffer((JbTokenStream ts, int maxLA) -> new ArrayTokenBuffer(ts, maxLA), 2);
    }

    @Test
    public void testWindowTokenBuffer() throws Exception {
        doTestTokenBuffer((JbTokenStream ts, int maxLA) -> new WindowTokenBuffer(ts, maxLA, 1024), 2);
        doTestTokenBuffer((JbTokenStream ts, int maxLA) -> new WindowTokenBuffer(ts, maxLA, 3), 2);
    }

    @Test
    public void testParserSimplest() throws Exception {
        String text = 
                "var x = 500\n" +
                "var y = 3.14\n" +
                "var z = \"qwe\"\n" +
                "print \"x = \"\n" +
                "out x\n";
        JbTokenStream ts = lex(text);
        //printTokens(ts);
        //ts = lex(text);
        JbNode ast = new Parser().parse(ts);
        printAst(ast);
    }

    @Test
    public void testParserFromTZ() throws Exception {
        String text = "\n" +
            "var n = 500\n" +
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" +
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "print \"pi = \"\n" +
            "out pi";
        JbTokenStream ts = lex(text);
        //printTokens(ts);
        //ts = lex(text);
        JbNode ast = new Parser().parse(ts);
        printAst(ast);
    }
}
