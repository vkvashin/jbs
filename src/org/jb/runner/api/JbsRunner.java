package org.jb.runner.api;

import java.io.PrintStream;
import org.jb.ast.api.ASTNode;
import org.jb.ast.diagnostics.DiagnosticListener;
import org.jb.runner.impl.RunnerImpl;

/**
 * Executes AST
 * @author vkvashin
 */
public final class JbsRunner {
    
    private final RunnerImpl runnerImpl;

    public JbsRunner(PrintStream out, PrintStream err, DiagnosticListener diagnosticListener) {
        runnerImpl = new RunnerImpl(out, err, diagnosticListener);
    }
    
    public void execute(ASTNode ast) {
        runnerImpl.execute(ast);
    }
}
