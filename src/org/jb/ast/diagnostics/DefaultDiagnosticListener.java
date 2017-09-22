package org.jb.ast.diagnostics;

/**
 *
 * @author vkvashin
 */
public class DefaultDiagnosticListener implements DiagnosticListener {

    @Override
    public void report(Diagnostic issue) {
        System.err.println(issue.getDisplayText());
    }
    
    @Override
    public void error(Exception e) {
        e.printStackTrace();
    }
}
