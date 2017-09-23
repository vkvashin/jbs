package org.jb.ui;

import com.sun.java.accessibility.util.SwingEventMonitor;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author vkvashin
 */
/*package*/ class EditorWindow extends JPanel {

    private final JTextPane textArea;
    private final JScrollPane scroller;
    
    public EditorWindow() {
        textArea = new JTextPane();
        textArea.setFont(new Font("monospaced", Font.PLAIN, 14));
        scroller = new JScrollPane(textArea);
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
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
        });
    }
    
    private void documentUpdated() {        
    }
    
    public String getText() {
        assert SwingUtilities.isEventDispatchThread();
        return textArea.getText();
    }
}
