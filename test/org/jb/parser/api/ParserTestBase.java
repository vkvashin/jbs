/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.api;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.jb.ast.api.ASTNode;
import org.jb.ast.api.Expr;
import org.jb.lexer.api.LexerTestBase;
import org.jb.lexer.api.TokenStream;
import static org.junit.Assert.assertTrue;
import org.junit.Before;

/**
 *
 * @author vkvashin
 */
public class ParserTestBase extends LexerTestBase {
    
    private boolean printTypes = false;

    public class AstPrinter {

        private final int step = 4;
        private final StringBuilder indentBuffer = new StringBuilder();
        private final PrintStream ps;

        public AstPrinter(PrintStream ps) {
            this.ps = ps;
        }

        private void indent() {
            setupIndentBuffer(indentBuffer.length() + step);
        }

        public void unindent() {
            setupIndentBuffer(indentBuffer.length() - step);
        }

        private void setupIndentBuffer(int len) {
            if (len <= 0) {
                indentBuffer.setLength(0);
            } else {
                indentBuffer.setLength(len);
                for (int i = 0; i < len; i++) {
                    indentBuffer.setCharAt(i, ' ');
                }
            }
        }

        public void printAst(ASTNode ast) {
            while (ast != null) {
                ps.append(indentBuffer).append(toString(ast)); 
                if (printTypes && ast instanceof Expr) {
                    ps.append(' ').append(((Expr) ast).getType().getDisplayName());
                }
                ps.append('\n');
                ASTNode firstChild = ast.getFirstChild();
                if (firstChild != null) {
                    indent();
                    printAst(firstChild);
                    unindent();
                }
                ast = ast.getNextSibling();
            }
        }

        private CharSequence toString(ASTNode ast) {
            return ast.toString();
        }
    }

    @Before
    public void setUp() {
        printTypes = false;
    }

    public void setPrintTypes(boolean printTypes) {
        this.printTypes = printTypes;
    }
    
    protected void printAst(ASTNode ast) {
        printAst(ast, System.out);
    }

    protected void printAst(ASTNode ast, PrintStream ps) {
        new AstPrinter(ps).printAst(ast);
    }

    protected CharSequence getAstDump(ASTNode ast) {
        StringPrintStream ps = new StringPrintStream();
        new AstPrinter(ps).printAst(ast);
        return ps;
    }

    protected String[] getAstDumpLines(ASTNode ast) {
        StringPrintStream ps = new StringPrintStream();
        new AstPrinter(ps).printAst(ast);
        return ps.toString().split("\n");
    }

    protected void doTestAST(String source, String[] expected) throws Exception {        
        doTestAST(getAst(source), expected);
    }

    protected void doTestAST(ASTNode ast, String[] expected) throws Exception {        
        String[] actual = getAstDumpLines(ast);
        if (expected != null) {
            assertEquals("AST dump differs", expected, actual);
        }
    }

    protected List<ASTNode> getAstAsList(String source) throws Exception {
        return getAstAsList(getAst(source));
    }

    protected List<ASTNode> getAstAsList(ASTNode ast) throws Exception {
        List<ASTNode> l = new ArrayList<>();
        while (ast != null) {
            l.add(ast);
            ast = ast.getNextSibling();
        }
        return l;
    }

    protected ASTNode getAst(String source) throws Exception {
        TokenStream ts = lex(source);
        ASTNode ast = new Parser().parse(ts, getTestDiagnosticListener());
        if (isDebug()) {
            printAst(ast);
        }
        return ast;
    }

    protected void assertEquals(String message, String[] expected, String[] actual) {
        for (int i = 0; i < expected.length; i++) {
            if (i < actual.length) {
                if (!expected[i].equals(actual[i])) {
                    assertAndPrint(message + ": line " + i + " differs: expected vs actual is\n" + expected[i] + "\n" + actual[i], actual);
                }
            } else {
                assertAndPrint(message + " premature end of output", actual);
            }
        }
        if (expected.length < actual.length) {
            assertAndPrint(message + " actual output is longer by " + (actual.length - expected.length) + " lines", actual);
        }
    }

    private void assertAndPrint(String assertionMessage, String[] linesToPrint) {
        StringBuilder sb = new StringBuilder(assertionMessage);
        sb.append("\nFull dump:");
        for (String l : linesToPrint) {
            sb.append('\n').append(l);
        }
        assertTrue(sb.toString(), false);
    }

    private static class StringPrintStream extends PrintStream implements CharSequence {

        private ByteArrayOutputStream os;

        public StringPrintStream() {
            this(new ByteArrayOutputStream());
        }

        private StringPrintStream(ByteArrayOutputStream arr) {
            super(arr);
            os = arr;
        }

        public int length() {
            return toString().length();
        }

        public char charAt(int index) {
            return toString().charAt(index);
        }

        public CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }

        @Override
        public String toString() {
            return os.toString();
        }
    }
}

