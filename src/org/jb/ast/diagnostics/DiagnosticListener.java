package org.jb.ast.diagnostics;

/**
 *
 * @author vkvashin
 */
public interface DiagnosticListener {
    void report(Diagnostic issue);    
}
