package org.jb.runner.impl;

import java.io.PrintStream;
import org.jb.ast.api.ASTNode;
import org.jb.ast.diagnostics.DiagnosticListener;

/**
 *
 * @author vkvashin
 */
public class RunnerImpl {
    
    private final PrintStream out;
    private final PrintStream err;
    private final DiagnosticListener diagnosticListener;

    public RunnerImpl(PrintStream out, PrintStream err, DiagnosticListener diagnosticListener) {
        this.out = out;
        this.err = err;
        this.diagnosticListener = diagnosticListener;
    }

    public void execute(ASTNode ast) {        
    }
}
