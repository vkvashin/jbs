/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.api;

import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import org.jb.ast.api.ASTNode;
import org.jb.lexer.api.Token;
import org.jb.lexer.api.TokenStreamException;
import org.jb.parser.impl.TokenBuffer;
import org.jb.parser.impl.ArrayTokenBuffer;
import org.jb.parser.impl.WindowTokenBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jb.lexer.api.TokenStream;

/**
 *
 * @author vkvashin
 */
public class ParserSimpleTest extends ParserTestBase {

    public ParserSimpleTest() {
    }

    @FunctionalInterface
    private interface TokenBufferFactory {
        TokenBuffer create(TokenStream ts, int maxLA) throws TokenStreamException;
    }

    public void doTestTokenBuffer(TokenBufferFactory factory, int maxLA) throws Exception {
        String text = "\n" +
            "var n = 500\n" +
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" +
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "print \"pi = \"\n" +
            "out pi";
        
        Token[] refTokens = lexAndGetTokenArray(text);
        TokenBuffer tb = factory.create(lex(text), maxLA);
        for (int base = 0; base < refTokens.length; base++) {
            for(int la = 0; la < maxLA; la++) {
                int idx = base + la;
                if (idx < refTokens.length) {
                    Token tok = tb.LA(la);
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
        doTestTokenBuffer((TokenStream ts, int maxLA) -> new ArrayTokenBuffer(ts, maxLA), 2);
    }

    @Test
    public void testWindowTokenBuffer() throws Exception {
        doTestTokenBuffer((TokenStream ts, int maxLA) -> new WindowTokenBuffer(ts, maxLA, 1024), 2);
        doTestTokenBuffer((TokenStream ts, int maxLA) -> new WindowTokenBuffer(ts, maxLA, 3), 2);
    }

    @Test
    public void testParserSimplest() throws Exception {
        String source =
                "var x = 500\n" +
                "var y = 3.14\n" +
                "var z = \"qwe\"\n" +
                "var w = x + 1\n" +
                "print \"x = \"\n" +
                "out x\n";
        String[] expected = new String[] {
            "DECL [1:1] x",
            "    INT [1:9] 500",
            "DECL [2:1] y",
            "    FLOAT [2:9] 3.14",
            "DECL [3:1] z",
            "    STRING [3:9] qwe",
            "DECL [4:1] w",
            "    OP [4:9] +",
            "        ID [4:9] x",
            "        INT [4:13] 1",
            "PRINT [5:1] ",
            "    STRING [5:7] x = ",
            "OUT [6:1] ",
            "    ID [6:5] x"
        };
        doTestAST(source, expected);
    }

    @Test
    public void testParserFromTZ() throws Exception {
        String source =
            "var n = 500\n" +
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" +
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "print \"pi = \"\n" +
            "out pi";
        String[] expected = new String[] {
            "DECL [1:1] n",
            "    INT [1:9] 500",
            "DECL [2:1] sequence",
            "    MAP [2:16] ",
            "        SEQ [2:20] ",
            "            INT [2:21] 0",
            "            ID [2:24] n",
            "        ID [2:28] i",
            "        OP [2:33] ^",
            "            PAREN [2:33] ",
            "                INT [2:34] -1",
            "            OP [2:38] /",
            "                ID [2:38] i",
            "                PAREN [2:42] ",
            "                    OP [2:43] *",
            "                        FLOAT [2:43] 2.0",
            "                        OP [2:49] +",
            "                            ID [2:49] i",
            "                            INT [2:53] 1",
            "DECL [3:1] pi",
            "    OP [3:10] *",
            "        INT [3:10] 4",
            "        MAP [3:14] ",
            "            ID [3:21] sequence",
            "            INT [3:31] 0",
            "            ID [3:34] x",
            "            ID [3:36] y",
            "            OP [3:41] +",
            "                ID [3:41] x",
            "                ID [3:45] y",
            "PRINT [4:1] ",
            "    STRING [4:7] pi = ",
            "OUT [5:1] ",
            "    ID [5:5] pi"
        };
        doTestAST(source, expected);
//        TokenStream ts = lex(source);
//        //printTokens(ts);
//        //ts = lex(text);
//        ASTNode ast = new Parser().parse(ts);
//        printAst(ast);
    }
}
