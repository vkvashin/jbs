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
import java.util.List;
import org.jb.ast.api.ASTNode;
import org.jb.ast.diagnostics.Diagnostic;
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
                    if (Token.isEOF(tok)) {
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
        assertEmptyDiagnostics();
    }

    @Test
    public void testWindowTokenBuffer() throws Exception {
        doTestTokenBuffer((TokenStream ts, int maxLA) -> new WindowTokenBuffer(ts, maxLA, 1024), 2);
        assertEmptyDiagnostics();
        doTestTokenBuffer((TokenStream ts, int maxLA) -> new WindowTokenBuffer(ts, maxLA, 3), 2);
        assertEmptyDiagnostics();
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
        assertEmptyDiagnostics();
    }

    @Test
    public void testSimplestErrorProcessing() throws Exception {
        String source =
                "var x = 5.4.3\n" + // lexer error
                "var y = 3\n" +
                "var w = y + 1\n" +
                "123 some shit 456\n" + // parser error
                "print \"x = \"\n" +
                "out x\n" +
                "print \"unterminated\n"; // lexer error
        String[] expected = new String[] {
            "DECL [1:1] x",
            "    FLOAT [1:9] 5.4",
            "DECL [2:1] y",
            "    INT [2:9] 3",
            "DECL [3:1] w",
            "    OP [3:9] +",
            "        ID [3:9] y",
            "        INT [3:13] 1",
            "PRINT [5:1] ",
            "    STRING [5:7] x = ",
            "OUT [6:1] ",
            "    ID [6:5] x",
            "PRINT [7:1] ",
            "    STRING [7:7] unterminated"
        };
        setDebug(true);
        doTestAST(source, expected);
        assertDiagnosticEquals(0, 1, 9, "more than one digital point");
        assertDiagnosticEquals(1, 4, 1, "unexpected token 123");
        assertDiagnosticEquals(2, 8, 1, "unterminated string");        
    }

    @Test
    public void testTypeErrorProcessing() throws Exception {
        String source =
                "var x = 1 + 2.7\n" + 
                "var y = 3.14 + \"asdf\"\n";
        setDebug(true);
        doTestAST(source, null);
        assertNonEmptyDiagnostics();
    }
    
    @Test
    public void testParsePrecedenceMulAdd() throws Exception {
        // * stronger than +s
        String source = "var a = 2*3+4";
        String[] expected = new String[] {
            "DECL [1:1] a",
            "    OP [1:9] +",
            "        OP [1:9] *",
            "            INT [1:9] 2",
            "            INT [1:11] 3",
            "        INT [1:13] 4"
        };
        //printAst(new Parser().parse(lex(source)));
        doTestAST(source, expected);
        assertEmptyDiagnostics();
    }

    @Test
    public void testParsePrecedenceMulPow() throws Exception {
        // * stronger than +
        String source = "var a = 2*3^4";
        String[] expected = new String[] {
            "DECL [1:1] a",
            "    OP [1:9] *",
            "        INT [1:9] 2",
            "        OP [1:11] ^",
            "            INT [1:11] 3",
            "            INT [1:13] 4"
        };
        //printAst(new Parser().parse(lex(source)));
        doTestAST(source, expected);
        assertEmptyDiagnostics();
    }

    @Test
    public void testSimpleMap1() throws Exception {
        String source
                = "var n = 500\n"
                + "var sequence = map({0, n}, i -> i*2)\n";
        String[] expected = new String[]{
            "DECL [1:1] n",
            "    INT [1:9] 500",
            "DECL [2:1] sequence",
            "    MAP [2:16] ",
            "        SEQ [2:20] ",
            "            INT [2:21] 0",
            "            ID [2:24] n",
            "        DECL [2:28] i",
            "        OP [2:33] *",
            "            ID [2:33] i",
            "            INT [2:35] 2"
        };
        setDebug(true);
        doTestAST(source, expected);
        assertEmptyDiagnostics();
    }
    
    @Test
    public void testSimpleMap2() throws Exception {
        String source =
            "var n = 500\n" +
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n";
        String[] expected = new String[] {
            "DECL [1:1] n",
            "    INT [1:9] 500",
            "DECL [2:1] sequence",
            "    MAP [2:16] ",
            "        SEQ [2:20] ",
            "            INT [2:21] 0",
            "            ID [2:24] n",
            "        DECL [2:28] i",
            "        OP [2:33] /",
            "            OP [2:33] ^",
            "                PAREN [2:33] ",
            "                    INT [2:34] -1",
            "                ID [2:38] i",
            "            PAREN [2:42] ",
            "                OP [2:43] +",
            "                    OP [2:43] *",
            "                        FLOAT [2:43] 2.0",
            "                        ID [2:49] i",
            "                    INT [2:53] 1"
        };
        //setDebug(true);
        doTestAST(source, expected);
        assertEmptyDiagnostics();
    }    

    @Test
    public void testSimpleMapWithSameVarName() throws Exception {
        String source
                = "var n = 500\n"
                + "var s = map({0, n}, n -> n*2)\n";
        String[] expected = new String[]{
            "DECL [1:1] n",
            
        };
        setDebug(true);
        doTestAST(source, null);
        assertEmptyDiagnostics();
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
            "        DECL [2:28] i",
            "        OP [2:33] /",
            "            OP [2:33] ^",
            "                PAREN [2:33] ",
            "                    INT [2:34] -1",
            "                ID [2:38] i",
            "            PAREN [2:42] ",
            "                OP [2:43] +",
            "                    OP [2:43] *",
            "                        FLOAT [2:43] 2.0",
            "                        ID [2:49] i",
            "                    INT [2:53] 1",
            "DECL [3:1] pi",
            "    OP [3:10] *",
            "        INT [3:10] 4",
            "        REDUCE [3:14] ",
            "            ID [3:21] sequence",
            "            INT [3:31] 0",
            "            DECL [3:34] x",
            "            DECL [3:36] y",
            "            OP [3:41] +",
            "                ID [3:41] x",
            "                ID [3:45] y",
            "PRINT [4:1] ",
            "    STRING [4:7] pi = ",
            "OUT [5:1] ",
            "    ID [5:5] pi"
        };
        //setDebug(true);
        doTestAST(source, expected);
        assertEmptyDiagnostics();
    }
    
    @Test
    public void testSimpleReduce1() throws Exception {
        String source = 
            "var x = {1, 3}\n" + 
            "var y = reduce(x, 0, x y -> x+y)\n" + 
            "out y\n";
        setDebug(true);
        String[] expected = new String[] {
            "DECL [1:1] x",
            "    SEQ [1:9] ",
            "        INT [1:10] 1",
            "        INT [1:13] 3",
            "DECL [2:1] y",
            "    REDUCE [2:9] ",
            "        ID [2:16] x",
            "        INT [2:19] 0",
            "        DECL [2:22] x",
            "        DECL [2:24] y",
            "        OP [2:29] +",
            "            ID [2:29] x",
            "            ID [2:31] y",
            "OUT [3:1] ",
            "    ID [3:5] y"
        };
        doTestAST(source, expected);
        assertEmptyDiagnostics();
    }    
    
    @Test
    public void testSimpleReduceErrorVarsAreSame() throws Exception {
        String source = 
            "var x = {1, 3}\n" + 
            "var y = reduce(x, 0, x x -> x+1)\n" + 
            "out y\n";
        setDebug(true);
        doTestAST(source, null);
        assertDiagnosticEquals(0, 2, 24, "duplicate variable declaration x");
    }    
}
