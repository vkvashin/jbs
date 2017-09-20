/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.api;

import java.io.PrintStream;
import org.jb.ast.api.ASTNode;
import org.jb.lexer.api.LexerTestBase;

/**
 *
 * @author vkvashin
 */
public class ParserTestBase extends LexerTestBase {

    public static class AstPrinter {

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
                ps.append(indentBuffer).append(toString(ast)).append('\n');
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

    protected void printAst(ASTNode ast) {
        new AstPrinter(System.out).printAst(ast);
    }
}
