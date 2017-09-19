package org.jb.lexer.api;


import java.io.StringBufferInputStream;
import org.*;
import org.*;
import org.junit.Test;
import org.jb.lexer.api.JbTokenStream;
import org.jb.lexer.api.JbLexer;
import org.junit.Assert;

/**
 *
 * @author vkvashin
 */
public class LexerSimpleTest  extends LexerTestBase {

    @Test
    public void simpleTest1() throws Exception {
        doSimpleTest("(){},=", createRefTokens(
            JbToken.Kind.LPAREN, null, 1, 1,
            JbToken.Kind.RPAREN, null, 1, 2,
            JbToken.Kind.LCURLY, null, 1, 3,
            JbToken.Kind.RCURLY, null, 1, 4,            
            JbToken.Kind.COMMA,  null, 1, 5,
            JbToken.Kind.EQ,     null, 1, 6
        ));
    }
    
    @Test
    public void simpleTestNumbers1() throws Exception {
        doSimpleTest("1 2 3.14 -2.7", createRefTokens(
            JbToken.Kind.INT, "1", 1, 1,
            JbToken.Kind.INT, "2", 1, 3,
            JbToken.Kind.FLOAT, "3.14", 1, 5,
            JbToken.Kind.FLOAT, "-2.7", 1, 10
        ));
    }
    
    @Test
    public void simpleTestNumbers2() throws Exception {
        try {
            doSimpleTest("1.2.3", new JbToken[1]);
            Assert.assertTrue("JbTokenStreamException should be generated", false);
        } catch (JbTokenStreamException ex) {
            // OK
        }
    }    
    
    @Test
    public void simpleTestOps() throws Exception {
        doSimpleTest("+-*/^", createRefTokens(
            JbToken.Kind.ADD, null, 1, 1,
            JbToken.Kind.SUB, null, 1, 2,
            JbToken.Kind.MUL, null, 1, 3,
            JbToken.Kind.DIV, null, 1, 4,
            JbToken.Kind.POW, null, 1, 5
        ));
    }
    
    @Test
    public void simpleTestString() throws Exception {
        doSimpleTest("\"qwe\" \"asd\nzxc\" \"\"", createRefTokens(
            JbToken.Kind.STRING, "qwe", 1, 1,
            JbToken.Kind.STRING, "asd\nzxc", 1, 7,
            JbToken.Kind.STRING, "", 2, 6
        ));
    }

    @Test
    public void simpleTestIdsAndFunctions() throws Exception {
        doSimpleTest("var1 map reduce print out", createRefTokens(
            JbToken.Kind.ID, "var1", 1, 1,
            JbToken.Kind.MAP, null, 1, 6,
            JbToken.Kind.REDUCE, null, 1, 10,
            JbToken.Kind.PRINT, null, 1, 17,
            JbToken.Kind.OUT, null, 1, 23
        ));
    }
    
    @Test
    public void testFromTZ() throws Exception {
        String text = "\n" +
            "var n = 500\n" +
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" +
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" + 
            "print \"pi = \"\n" +
            "out pi";
        lexAndPrint(text);
        doSimpleTest(text, createRefTokens(
            JbToken.Kind.VAR, null, 2, 1, 
            JbToken.Kind.ID, "n" , 2, 5, 
            JbToken.Kind.EQ, null, 2, 7, 
            JbToken.Kind.INT, "500" , 2, 9, 
            JbToken.Kind.VAR, null, 3, 1, 
            JbToken.Kind.ID, "sequence", 3, 5, 
            JbToken.Kind.EQ, null , 3, 14, 
            JbToken.Kind.MAP, null, 3, 16, 
            JbToken.Kind.LPAREN, null , 3, 19, 
            JbToken.Kind.LCURLY, null, 3, 20, 
            JbToken.Kind.INT, "0" , 3, 21, 
            JbToken.Kind.COMMA, null, 3, 22, 
            JbToken.Kind.ID, "n", 3, 24, 
            JbToken.Kind.RCURLY, null, 3, 25, 
            JbToken.Kind.COMMA, null, 3, 26, 
            JbToken.Kind.ID, "i", 3, 28, 
            JbToken.Kind.ARROW, null, 3, 30, 
            JbToken.Kind.LPAREN, null, 3, 33, 
            JbToken.Kind.INT, "-1" , 3, 34, 
            JbToken.Kind.RPAREN, null, 3, 36, 
            JbToken.Kind.POW, null, 3, 37, 
            JbToken.Kind.ID, "i" , 3, 38, 
            JbToken.Kind.DIV, "/", 3, 40, 
            JbToken.Kind.LPAREN, null, 3, 42, 
            JbToken.Kind.FLOAT, "2.0", 3, 43, 
            JbToken.Kind.MUL, null, 3, 47, 
            JbToken.Kind.ID, "i", 3, 49, 
            JbToken.Kind.ADD, null, 3, 51, 
            JbToken.Kind.INT, "1", 3, 53, 
            JbToken.Kind.RPAREN, null, 3, 54, 
            JbToken.Kind.RPAREN, null, 3, 55, 
            JbToken.Kind.VAR, null, 4, 1, 
            JbToken.Kind.ID, "pi", 4, 5, 
            JbToken.Kind.EQ, null, 4, 8, 
            JbToken.Kind.INT, "4", 4, 10, 
            JbToken.Kind.MUL, null, 4, 12, 
            JbToken.Kind.REDUCE, null, 4, 14, 
            JbToken.Kind.LPAREN, null, 4, 20, 
            JbToken.Kind.ID, "sequence", 4, 21, 
            JbToken.Kind.COMMA, null, 4, 29, 
            JbToken.Kind.INT, "0", 4, 31, 
            JbToken.Kind.COMMA, null, 4, 32, 
            JbToken.Kind.ID, "x", 4, 34, 
            JbToken.Kind.ID, "y", 4, 36, 
            JbToken.Kind.ARROW, null, 4, 38, 
            JbToken.Kind.ID, "x", 4, 41, 
            JbToken.Kind.ADD, null, 4, 43, 
            JbToken.Kind.ID, "y", 4, 45, 
            JbToken.Kind.RPAREN, null, 4, 46, 
            JbToken.Kind.PRINT, null, 5, 1, 
            JbToken.Kind.STRING, "pi = "  , 5, 7, 
            JbToken.Kind.OUT, null, 6, 1, 
            JbToken.Kind.ID, "pi", 6, 5                 
        ));
    }   
}
