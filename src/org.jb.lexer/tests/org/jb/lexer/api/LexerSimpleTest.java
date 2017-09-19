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
            JbToken.Kind.LPAREN, "(", 1, 1,
            JbToken.Kind.RPAREN, ")", 1, 2,
            JbToken.Kind.LCURLY, "{", 1, 3,
            JbToken.Kind.RCURLY, "}", 1, 4,            
            JbToken.Kind.COMMA,  ",", 1, 5,
            JbToken.Kind.EQ,     "=", 1, 6
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
            JbToken.Kind.ADD, "+", 1, 1,
            JbToken.Kind.SUB, "-", 1, 2,
            JbToken.Kind.MUL, "*", 1, 3,
            JbToken.Kind.DIV, "/", 1, 4,
            JbToken.Kind.POW, "^", 1, 5
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
            JbToken.Kind.MAP, "map", 1, 6,
            JbToken.Kind.REDUCE, "reduce", 1, 10,
            JbToken.Kind.PRINT, "print", 1, 17,
            JbToken.Kind.OUT, "out", 1, 23
        ));
    }
}
