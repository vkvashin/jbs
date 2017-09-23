package org.jb.ui;

import com.sun.java.accessibility.util.SwingEventMonitor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    private final JTextPane textArea;
    private final JScrollPane scroller;
    private final javax.swing.Timer docUpdateTimer;
    private final ErrorHighlightPainter errorHighlightPainter;
    
    /**
     * Incremented each time we schedule a syntax check.
     * When the check is done in a separate thread, and we switch to EDT to display errors,
     * we check whether this number is still the same. If it is not, we won't show errors -
     * they are already outdated.
     * 
     * Should be accessed from EDT only! 
     */
    private int updateId = 0;

    public EditorWindow() {
        textArea = new JTextPane();
        textArea.setFont(new Font("monospaced", Font.PLAIN, 14));
        scroller = new JScrollPane(textArea);
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
        errorHighlightPainter = new ErrorHighlightPainter();
        docUpdateTimer = new javax.swing.Timer(2000, new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                assert SwingUtilities.isEventDispatchThread();
                Controller.getInstance().scheduleSyntaxCheck(textArea.getText(), ++updateId);
            }
        });
        docUpdateTimer.setRepeats(false);
        textArea.getDocument().addDocumentListener(new DocumentListener() {            
            @Override
            public void insertUpdate(DocumentEvent e) {
                documentUpdated();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                documentUpdated();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                documentUpdated();
            }
            private void documentUpdated() {
                // Here we use javax.swing.Timer instead of java.util.concurrency staff
                // because javax.swing.Timer raises its event in EDT.
                // This allows us not to safely make editor content copy
                // and at the same time to do this only when necessary.
                docUpdateTimer.stop();
                docUpdateTimer.start();
            }
        });
    }

    public String getText() {
        assert SwingUtilities.isEventDispatchThread();
        return textArea.getText();
    }
    
    public void underlineErrors(List<Diagnostic> errors, final int updateId) {
        if (SwingUtilities.isEventDispatchThread()) {
            underlineErrorsImpl(errors, updateId);
        } else {
            SwingUtilities.invokeLater(() -> underlineErrorsImpl(errors, updateId));
        }
    }

    public void underlineErrorsImpl(List<Diagnostic> errors, final int updateId) {
        assert SwingUtilities.isEventDispatchThread();
        if (updateId != this.updateId) {
            return;
        }
        Highlighter highlighter = textArea.getHighlighter();
        highlighter.removeAllHighlights();
        if (errors.isEmpty()) {
            return;
        }
        ArrayList<Diagnostic> sorted = new ArrayList<>(errors);
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
        Document doc = textArea.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            int lastOffset = 0;
            int lastLine = 1; // lines in diagnostics are 1-based
            for (Diagnostic error : sorted) {
                final int line = error.getLine();
                while (line > lastLine) {
                    char c = text.charAt(++lastOffset);
                    if (c == '\n') {
                        lastLine++;
                    }
                }
                // here the lastOffset is the offset of the line in text
                int offset = lastOffset + error.getColumn();
                int start = javax.swing.text.Utilities.getWordStart(textArea, offset);
                int end = javax.swing.text.Utilities.getWordEnd(textArea, offset);
                highlighter.addHighlight(start, end, errorHighlightPainter);
            }
        } catch (BadLocationException ex) {
            Logger.getLogger(EditorWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private class ErrorHighlightPainter implements HighlightPainter {
        private final Color errorColor = new Color(200, 25, 25);
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            g.setColor(errorColor);
            try {
                Rectangle start = textArea.modelToView(p0);
                Rectangle end = textArea.modelToView(p1);
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
}

