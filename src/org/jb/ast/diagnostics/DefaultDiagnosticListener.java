package org.jb.ast.diagnostics;

import java.io.PrintStream;

/**
 *
 * @author vkvashin
 */
public class DefaultDiagnosticListener implements DiagnosticListener {

    private DefaultDiagnosticListener() {
    }

    @Override
    public void report(Diagnostic issue) {
        report(issue, System.err);
    }
    
    public static void report(Diagnostic issue, PrintStream printStream) {
        System.err.println(issue.getDisplayText());
        for (Diagnostic child = issue.getChained(); child != null; child = child.getChained()) {
            System.err.println(child.getDisplayText());
        }
    }
    
    private static volatile DiagnosticListener INSTANCE = new DefaultDiagnosticListener();

    public static DiagnosticListener getDefaultListener() {
        return INSTANCE;
    }
    
    public static void setDefaultListener(DiagnosticListener listener) {
        INSTANCE = listener;
    }
    public static void resetDefaultListener() {
        INSTANCE = new DefaultDiagnosticListener();
    }
}
