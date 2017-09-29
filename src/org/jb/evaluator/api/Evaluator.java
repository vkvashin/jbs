package org.jb.evaluator.api;

import java.io.PrintStream;
import org.jb.ast.api.ASTNode;
import org.jb.ast.diagnostics.DiagnosticListener;
import org.jb.evaluator.impl.EvaluatorImpl;

/**
 * Executes AST
 * @author vkvashin
 */
public final class Evaluator {
    
    private final EvaluatorImpl runnerImpl;

    public Evaluator(Appendable out, DiagnosticListener... diagnosticListeners) {
        runnerImpl = new EvaluatorImpl(out, diagnosticListeners);
    }
    
    public void execute(ASTNode ast) {
        runnerImpl.execute(ast);
    }

    public void dispose() {
        runnerImpl.dispose();
    }
}
