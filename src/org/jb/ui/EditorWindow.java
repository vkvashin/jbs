package org.jb.ui;

import com.sun.java.accessibility.util.SwingEventMonitor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jb.ast.diagnostics.Diagnostic;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Component for editing script source code
 * @author vkvashin
 */
/*package*/ class EditorWindow extends JPanel {

    private final EditorPane editorPane;
    private final JScrollPane scroller;
    private final ErrorHighlighter errorHighlighter;

    public EditorWindow() {
        editorPane = new EditorPane();
        editorPane.setFont(new Font("monospaced", Font.PLAIN, 14));
        errorHighlighter = new ErrorHighlighter(editorPane.getHighlighter());
        scroller = new JScrollPane(editorPane);
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
    }

    public String getText() {
        assert SwingUtilities.isEventDispatchThread();
        return editorPane.getText();
    }

    public void addDocumentListener(DocumentListener listener) {
        editorPane.getDocument().addDocumentListener(listener);
    }

    public void underlineErrors(List<Diagnostic> diagnostics) {
        assert SwingUtilities.isEventDispatchThread();
        if (diagnostics.isEmpty()) {
            errorHighlighter.setErrors(Collections.emptyList());
            return;
        }
        ArrayList<Diagnostic> sorted = new ArrayList<>(diagnostics);
        Collections.sort(sorted, new Comparator<Diagnostic>() {
            @Override
            public int compare(Diagnostic d1, Diagnostic d2) {
                if (d1.getLine() == d2.getLine()) {
                    return d1.getColumn()- d2.getColumn();
                } else {
                    return d1.getLine() - d2.getLine();
                }
            }
        });
        ArrayList<Error> errors = new ArrayList<>(diagnostics.size());
        Document doc = editorPane.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            int lastOffset = 0;
            int lastLine = 1; // lines in diagnostics are 1-based
            for (Diagnostic diag : sorted) {
                final int line = diag.getLine();
                while (line > lastLine) {
                    char c = text.charAt(++lastOffset);
                    if (c == '\n') {
                        lastLine++;
                    }
                }
                // here the lastOffset is the offset of the line in text
                int offset = lastOffset + diag.getColumn();
                int start = javax.swing.text.Utilities.getWordStart(editorPane, offset);
                int end = javax.swing.text.Utilities.getWordEnd(editorPane, offset);
                if ((end - start) == 1 && text.charAt(start) == '\n') {
                    start--;
                    end--;
                }
                errors.add(new Error(diag.getLevel(), start, end, diag.getMessage().toString()));
            }
        } catch (BadLocationException ex) {
            Logger.getLogger(EditorWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        errorHighlighter.setErrors(errors);
    }

    private class Error {
        public final Diagnostic.Level level;
        public final int startOffset;
        public final int endOffset;
        public final String message;
        public Error(Diagnostic.Level level, int startOffset, int endOffset, String message) {
            this.level = level;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.message = message;
        }
    }
    
    /** 
     * Wraps standard error highligter;
     * stores list of errors
     */
    private class ErrorHighlighter {
        
        private final Highlighter delegate;
        private final ErrorHighlightPainter errorHighlightPainter;

        /** 
         * A SORTED list of errors;
         * to be accessed in EDT only.
         */
        private List<Error> errors;
        
        public ErrorHighlighter(Highlighter delegate) {
            this.delegate = delegate;
            this.errorHighlightPainter = new ErrorHighlightPainter();
             errors = Collections.emptyList();
        }

        public List<Error> getErrors() {
            assert SwingUtilities.isEventDispatchThread();
            return errors;
        }

        public void setErrors(List<Error> errors) {
            assert SwingUtilities.isEventDispatchThread();
            this.errors = errors;
            delegate.removeAllHighlights();
            try {
                for (Error error : errors) {
                    delegate.addHighlight(error.startOffset, error.endOffset, errorHighlightPainter);
                }
            } catch (BadLocationException ex) { // should never happen
                Logger.getLogger(EditorWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class ErrorHighlightPainter implements HighlightPainter {
        private final Color errorColor = new Color(200, 25, 25);
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            g.setColor(errorColor);
            try {
                Rectangle start = editorPane.modelToView(p0);
                Rectangle end = editorPane.modelToView(p1);
                if (start.x < 0) {
                    return;
                }
                int waveLength = end.x + end.width - start.x;
                if (waveLength > 0) {
                    int[] wf = {0, 0, -1, -1};
                    int[] xArray = new int[waveLength + 1];
                    int[] yArray = new int[waveLength + 1];
                    int yBase = (int) (start.y + start.height - 2);
                    for (int i = 0; i <= waveLength; i++) {
                        xArray[i] = start.x + i;
                        yArray[i] = yBase + wf[xArray[i] % 4];
                    }
                    g.drawPolyline(xArray, yArray, waveLength);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class EditorPane extends JTextPane {

        public EditorPane() {
            ToolTipManager.sharedInstance().registerComponent(this);
        }
        
        @Override
        public String getToolTipText(MouseEvent ev) {
            List<Error> errors = errorHighlighter.getErrors();
            String tooltipText = null;
            if(!errors.isEmpty()) {
                Point pt = new Point(ev.getX(), ev.getY());
                int pos = viewToModel(pt);                
                for (Error err : errors) {
                    if (err.startOffset <= pos) {
                        if (err.endOffset >= pos) {
                            tooltipText = err.message;
                            break;
                        }
                    } else {
                        // errors are always sorted
                        break;
                    }
                }
            }   
            setToolTipText(tooltipText);
            return super.getToolTipText(ev);
        }
    }
}
