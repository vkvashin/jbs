package org.jb.ast.diagnostics;

/**
 *
 * @author vkvashin
 */
public class DefaultDiagnosticListener implements DiagnosticListener {

    @Override
    public void report(Diagnostic issue) {
        System.err.println(issue.getDisplayText());
        for(Diagnostic child = issue.getChained(); child != null; child = child.getChained()) {
            System.err.println(child.getDisplayText());
        }
    }
}
