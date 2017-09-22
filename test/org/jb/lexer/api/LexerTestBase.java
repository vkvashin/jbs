package org.jb.lexer.api;


import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.*;
import org.*;
import org.jb.ast.diagnostics.DefaultDiagnosticListener;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.ast.diagnostics.DiagnosticListener;
import org.junit.Test;
import org.jb.lexer.api.Lexer;
import org.jb.lexer.api.Token;
import org.junit.Assert;
import org.jb.lexer.api.TokenStream;
import org.junit.After;
import org.junit.Before;


/**
 *
 * @author vkvashin
 */
public class LexerTestBase {
    
    private boolean debug = false;
    
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final DiagnosticListener diagnosticListener = new DiagnosticListener() {
        DiagnosticListener defaultListener = new DefaultDiagnosticListener();                
        @Override
        public void report(Diagnostic issue) {
            defaultListener.report(issue);
            diagnostics.add(issue);
        }
    };

    public DiagnosticListener getTestDiagnosticListener() {
        return diagnosticListener;
    }
    
    @Before
    public void setUp() {
        diagnostics.clear();
        debug = false;
    }
    
    protected List<Diagnostic> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }
    
    protected void assertEmptyDiagnostics() {
        Assert.assertTrue(getDiagnostics().isEmpty());
    }

    protected void assertNonEmptyDiagnostics() {
        Assert.assertTrue(!getDiagnostics().isEmpty());
    }

    protected TokenStream lex(String text) {
        // StringBufferInputStream is deprecated since it does not work with non-ascii well.
        // TODO: change as soon as we test non ascii input
        StringBufferInputStream is = new StringBufferInputStream(text);
        return new Lexer(is, diagnosticListener).lex();
    }

    protected Token[] lexAndGetTokenArray(String text) throws Exception {
        TokenStream ts = lex(text);
        ArrayList<Token> tokens = new ArrayList<>();
        for (Token tok = ts.next(); !Token.isEOF(tok); tok = ts.next()) {
            tokens.add(tok);
        }
        return tokens.toArray(new Token[tokens.size()]);
    }
    
    protected void setDebug(boolean debug) {
        this.debug = debug;    
    }

    public boolean isDebug() {
        return debug;
    }

    protected void printTokens(TokenStream ts) throws Exception {
        Token tok;
        while(!Token.isEOF(tok = ts.next())) {
            System.out.println(tok);
        }        
    }

    protected void lexAndPrint(String text) throws Exception {
        String[] lines = text.split("\n");
        System.out.println("=== Printing tokens for " + lines[0] + (lines.length > 1 ? " ..." : ""));
        printTokens(lex(text));
    }

    protected void doSimpleTest(String text, Token...refTokens) throws Exception {
        if (debug) {
            String[] lines = text.split("\n");
            System.out.println("=== doSimpleTest " + lines[0] + (lines.length > 1 ? " ..." : ""));
        }
        TokenStream ts = lex(text);
        Token last = null;
        for (Token expected : refTokens) {
            Token actual = ts.next();
            if (debug) {
                System.out.println(actual);
            }
            Assert.assertNotNull("Premature end of token stream. Last token was " + last, actual);
            last = actual;
            assertEquals(expected, actual);
        }
    }

    protected Token[] createRefTokens(Object... args) {
        TokenFactory tokenFactory = new TokenFactoryImpl();
        assert args.length % 4 == 0;
        Token[] res = new Token[args.length / 4];
        for (int i = 0; i < args.length; i+=4) {
            Token.Kind kind = (Token.Kind) args[i];
            CharSequence text = (CharSequence) args[i+1];
            int line = ((Integer) args[i+2]).intValue();
            int column = ((Integer) args[i+3]).intValue();
            Token tok;
            tok = (text == null) ? tokenFactory.createFixed(kind, line, column) : tokenFactory.create(kind, text, line, column);           
            res[i / 4] = tok;
        }
        return res;
    }
        
    protected void assertEquals(Token expected, Token actual) {
        if (expected.getKind() != actual.getKind()
                || expected.getColumn() != actual.getColumn()
                || expected.getLine() != actual.getLine()
                || !expected.getText().toString().equals(actual.getText().toString())) {
            Assert.assertTrue("Tokens differ: expected " + expected + " but got " + actual, false);
        }
    }    
}
