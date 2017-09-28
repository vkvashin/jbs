package org.jb.ui;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import javax.swing.text.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.ast.diagnostics.DiagnosticListener;

/**
 *
 * @author vkvashin
 */
/*package*/ class OutputWindow extends JPanel {

    private final JTextPane textArea;
    private final JScrollPane scroller;
    private final JToolBar toolbar;
    private final DiagnosticListenerImpl diagnosticListener;    
    private final Color errorColor;
    
    public OutputWindow() {
        textArea = new JTextPane();
        textArea.setFont(new Font("monospaced", Font.PLAIN, 14));
        textArea.setEditable(false);
        scroller = new JScrollPane(textArea);
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
        toolbar = new JToolBar(JToolBar.VERTICAL);
        toolbar.setRollover(true);
        toolbar.add(Actions.RUN);
        toolbar.add(Actions.AST);
        toolbar.add(Actions.STOP);
        add(toolbar, BorderLayout.WEST);
        diagnosticListener = new DiagnosticListenerImpl();
        errorColor = new Color(200, 25, 25);
    }

    public DiagnosticListenerImpl getDiagnosticListener() {
        return diagnosticListener;
    }

    private class DiagnosticListenerImpl implements DiagnosticListener {
        @Override
        public void report(Diagnostic issue) {            
            printErr(issue.getDisplayText());
            for (Diagnostic child = issue.getChained(); child != null; child = child.getChained()) {
                printErr(child.getDisplayText());
            }
        }        
    }
    
    public void clear() {
        if (SwingUtilities.isEventDispatchThread()) {
            clearImpl();
        } else {
            SwingUtilities.invokeLater(() -> clearImpl());
        }
    }
    
    private void clearImpl() {
        assert SwingUtilities.isEventDispatchThread();
        Document doc = textArea.getDocument();
        try {
            textArea.getDocument().remove(0, doc.getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(OutputWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Appendable getOutputAsAppendable() {
        return new Appendable() {
            @Override
            public Appendable append(CharSequence text) throws IOException {
                printOut(text);
                return this;
            }

            @Override
            public Appendable append(CharSequence text, int start, int end) throws IOException {
                printOut(text.subSequence(start, end));
                return this;
            }

            @Override
            public Appendable append(char c) throws IOException {
                printOut("" + c); // ineffective, but I'm sure nobody uses it
                return this;
            }
        };
    }

    public void printOut(final CharSequence... text) {
        if (SwingUtilities.isEventDispatchThread()) {
            printOutImpl(text);
        } else {
            SwingUtilities.invokeLater(() -> printOutImpl(text));
        }
    }

    public void printErr(final CharSequence... text) {
        if (SwingUtilities.isEventDispatchThread()) {
            printErrImpl(text);
        } else {
            SwingUtilities.invokeLater(() -> printErrImpl(text));
        }
    }

    private void printErrImpl(CharSequence... text) {
        assert SwingUtilities.isEventDispatchThread();
        Document doc = textArea.getDocument();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setForeground(attr, errorColor);        
        try {
            for (CharSequence part : text) {
                doc.insertString(doc.getLength(), part.toString(), attr);
            }
            doc.insertString(doc.getLength(), "\n", attr);
        } catch (BadLocationException ex) {
            Logger.getLogger(OutputWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    private void printOutImpl(CharSequence... text) {
        assert SwingUtilities.isEventDispatchThread();
        Document doc = textArea.getDocument();
        try {
            for (CharSequence part : text) {
                doc.insertString(doc.getLength(), part.toString(), null);
            }
            //doc.insertString(doc.getLength(), "\n", null);
        } catch (BadLocationException ex) {
            Logger.getLogger(OutputWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
