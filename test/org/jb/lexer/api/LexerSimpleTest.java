package org.jb.lexer.api;


import org.*;
import org.*;
import org.junit.Test;
import org.jb.lexer.api.Token;
import org.jb.lexer.api.TokenStreamException;
import org.jb.lexer.api.LexerTestBase;
import org.junit.Assert;

/**
 *
 * @author vkvashin
 */
public class LexerSimpleTest  extends LexerTestBase {

    @Test
    public void simpleTest1() throws Exception {
        doSimpleTest("(){},=", createRefTokens(Token.Kind.LPAREN, null, 1, 1,
            Token.Kind.RPAREN, null, 1, 2,
            Token.Kind.LCURLY, null, 1, 3,
            Token.Kind.RCURLY, null, 1, 4,            
            Token.Kind.COMMA,  null, 1, 5,
            Token.Kind.EQ,     null, 1, 6
        ));
        assertEmptyDiagnostics();
    }
    
    @Test
    public void simpleTestNumbers1() throws Exception {
        doSimpleTest("1 2 3.14 -2.7", createRefTokens(Token.Kind.INT, "1", 1, 1,
            Token.Kind.INT, "2", 1, 3,
            Token.Kind.FLOAT, "3.14", 1, 5,
            Token.Kind.SUB, null, 1, 10,
            Token.Kind.FLOAT, "2.7", 1, 11
        ));
        assertEmptyDiagnostics();
    }
    
    @Test
    public void simpleTestNumbersWithErrors() throws Exception {
        setDebug(true);
        doSimpleTest("1.2.3 var y", createRefTokens(
                Token.Kind.FLOAT, "1.2", 1, 1,
                Token.Kind.VAR, null, 1, 7,
                Token.Kind.ID, "y", 1, 11
        ));
        assertNonEmptyDiagnostics();
    }    

    @Test
    public void simpleTestOps() throws Exception {
        doSimpleTest("+-*/^", createRefTokens(Token.Kind.ADD, null, 1, 1,
            Token.Kind.SUB, null, 1, 2,
            Token.Kind.MUL, null, 1, 3,
            Token.Kind.DIV, null, 1, 4,
            Token.Kind.POW, null, 1, 5
        ));
        assertEmptyDiagnostics();
    }
       
    @Test
    public void simpleTestString() throws Exception {
        doSimpleTest("\"qwe\" \"asd\nzxc\" \"\"", createRefTokens(Token.Kind.STRING, "qwe", 1, 1,
            Token.Kind.STRING, "asd\nzxc", 1, 7,
            Token.Kind.STRING, "", 2, 6
        ));
        assertEmptyDiagnostics();
    }

    @Test
    public void testUnterminatedString() throws Exception {
        doSimpleTest("\"qwe", createRefTokens(Token.Kind.STRING, "qwe", 1, 1));
        assertNonEmptyDiagnostics();
    }

    @Test
    public void simpleTestIdsAndFunctions() throws Exception {
        doSimpleTest("var1 map reduce print out", createRefTokens(Token.Kind.ID, "var1", 1, 1,
            Token.Kind.MAP, null, 1, 6,
            Token.Kind.REDUCE, null, 1, 10,
            Token.Kind.PRINT, null, 1, 17,
            Token.Kind.OUT, null, 1, 23
        ));
        assertEmptyDiagnostics();
    }

    @Test
    public void testPK() throws Exception {
        //setDebug(true);
        doSimpleTest("1-1", createRefTokens(
            Token.Kind.INT, "1", 1, 1,
            Token.Kind.SUB, null, 1, 2,
            Token.Kind.INT, "1", 1, 3
        ));
        assertEmptyDiagnostics();
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
        doSimpleTest(text, createRefTokens(Token.Kind.VAR, null, 2, 1, 
            Token.Kind.ID, "n" , 2, 5, 
            Token.Kind.EQ, null, 2, 7, 
            Token.Kind.INT, "500" , 2, 9, 
            Token.Kind.VAR, null, 3, 1, 
            Token.Kind.ID, "sequence", 3, 5, 
            Token.Kind.EQ, null , 3, 14, 
            Token.Kind.MAP, null, 3, 16, 
            Token.Kind.LPAREN, null , 3, 19, 
            Token.Kind.LCURLY, null, 3, 20, 
            Token.Kind.INT, "0" , 3, 21, 
            Token.Kind.COMMA, null, 3, 22, 
            Token.Kind.ID, "n", 3, 24, 
            Token.Kind.RCURLY, null, 3, 25, 
            Token.Kind.COMMA, null, 3, 26, 
            Token.Kind.ID, "i", 3, 28, 
            Token.Kind.ARROW, null, 3, 30, 
            Token.Kind.LPAREN, null, 3, 33, 
            Token.Kind.SUB, null , 3, 34,
            Token.Kind.INT, "1" , 3, 35,
            Token.Kind.RPAREN, null, 3, 36, 
            Token.Kind.POW, null, 3, 37, 
            Token.Kind.ID, "i" , 3, 38, 
            Token.Kind.DIV, "/", 3, 40, 
            Token.Kind.LPAREN, null, 3, 42, 
            Token.Kind.FLOAT, "2.0", 3, 43, 
            Token.Kind.MUL, null, 3, 47, 
            Token.Kind.ID, "i", 3, 49, 
            Token.Kind.ADD, null, 3, 51, 
            Token.Kind.INT, "1", 3, 53, 
            Token.Kind.RPAREN, null, 3, 54, 
            Token.Kind.RPAREN, null, 3, 55, 
            Token.Kind.VAR, null, 4, 1, 
            Token.Kind.ID, "pi", 4, 5, 
            Token.Kind.EQ, null, 4, 8, 
            Token.Kind.INT, "4", 4, 10, 
            Token.Kind.MUL, null, 4, 12, 
            Token.Kind.REDUCE, null, 4, 14, 
            Token.Kind.LPAREN, null, 4, 20, 
            Token.Kind.ID, "sequence", 4, 21, 
            Token.Kind.COMMA, null, 4, 29, 
            Token.Kind.INT, "0", 4, 31, 
            Token.Kind.COMMA, null, 4, 32, 
            Token.Kind.ID, "x", 4, 34, 
            Token.Kind.ID, "y", 4, 36, 
            Token.Kind.ARROW, null, 4, 38, 
            Token.Kind.ID, "x", 4, 41, 
            Token.Kind.ADD, null, 4, 43, 
            Token.Kind.ID, "y", 4, 45, 
            Token.Kind.RPAREN, null, 4, 46, 
            Token.Kind.PRINT, null, 5, 1, 
            Token.Kind.STRING, "pi = "  , 5, 7, 
            Token.Kind.OUT, null, 6, 1, 
            Token.Kind.ID, "pi", 6, 5                 
        ));
        assertEmptyDiagnostics();
    }   
}
