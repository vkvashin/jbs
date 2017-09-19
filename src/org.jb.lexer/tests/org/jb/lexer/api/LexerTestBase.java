package org.jb.lexer.api;


import java.io.StringBufferInputStream;
import org.*;
import org.*;
import org.junit.Test;
import org.jb.lexer.api.JbTokenStream;
import org.jb.lexer.api.JbLexer;
import org.jb.lexer.api.JbToken;
import org.junit.Assert;


/**
 *
 * @author vkvashin
 */
public class LexerTestBase {
    
    private boolean debug = false;

    protected JbTokenStream lex(String text) {
        // StringBufferInputStream is deprecated since it does not work with non-ascii well.
        // TODO: change as soon as we test non ascii input
        StringBufferInputStream is = new StringBufferInputStream(text);
        return new JbLexer(is).lex();
    }
    
    protected void setDebug(boolean debug) {
        this.debug = debug;    
    }

    protected void printTokens(JbTokenStream ts) throws Exception {
        JbToken tok;
        while((tok = ts.next()) != null) {
            System.out.println(tok);
        }        
    }

    protected void lexAndPrint(String text) throws Exception {
        String[] lines = text.split("\n");
        System.out.println("=== Printing tokens for " + lines[0] + (lines.length > 1 ? " ..." : ""));
        printTokens(lex(text));
    }

    protected void doSimpleTest(String text, JbToken...refTokens) throws Exception {
        if (debug) {
            String[] lines = text.split("\n");
            System.out.println("=== doSimpleTest " + lines[0] + (lines.length > 1 ? " ..." : ""));
        }
        JbTokenStream ts = lex(text);
        JbToken last = null;
        for (JbToken expected : refTokens) {
            JbToken actual = ts.next();
            if (debug) {
                System.out.println(actual);
            }
            Assert.assertNotNull("Premature end of token stream. Last token was " + last, actual);
            last = actual;
            assertEquals(expected, actual);
        }
    }

    protected JbToken[] createRefTokens(Object... args) {
        assert args.length % 4 == 0;
        JbToken[] res = new JbToken[args.length / 4];
        for (int i = 0; i < args.length; i+=4) {
            JbToken.Kind kind = (JbToken.Kind) args[i];
            CharSequence text = (CharSequence) args[i+1];
            int line = ((Integer) args[i+2]).intValue();
            int column = ((Integer) args[i+3]).intValue();
            JbToken tok;
            tok = (text == null) ? JbToken.createFixed(kind, line, column) : JbToken.create(kind, text, line, column);           
            res[i / 4] = tok;
        }
        return res;
    }
        
    protected void assertEquals(JbToken expected, JbToken actual) {
        if (expected.getKind() != actual.getKind()
                || expected.getColumn() != actual.getColumn()
                || expected.getLine() != actual.getLine()
                || !expected.getText().toString().equals(actual.getText().toString())) {
            Assert.assertTrue("Tokens differ: expected " + expected + " but got " + actual, false);
        }
    }    
}
