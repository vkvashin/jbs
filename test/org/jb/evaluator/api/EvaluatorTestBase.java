package org.jb.evaluator.api;

import org.jb.ast.api.*;
import org.jb.parser.api.*;

/**
 *
 * @author vkvashin
 */
public class EvaluatorTestBase extends ParserTestBase {

    protected String[] doTestEvaluator(String source, String...expected) throws Exception {
        ASTNode ast = getAst(source);
        StringBuilder out = new StringBuilder();
        Evaluator evaluator = new Evaluator(out, getTestDiagnosticListener());
        evaluator.execute(ast);
        evaluator.dispose();
        if (isDebug()) {
            System.out.println("\nEvaluator output");
            System.out.flush();
            System.out.println(out);
        }
        String[] actual = out.toString().split("\n");
        if (expected != null) {
            assertEquals("Executor output differs", expected, actual);
        }
        return actual;
    }
}
