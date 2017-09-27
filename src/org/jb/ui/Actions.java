package org.jb.ui;

import java.awt.event.*;
import java.awt.*;
import java.net.URL;
import javax.swing.*;

/**
 *
 * @author vkvashin
 */
public class Actions {
    
    /*package*/ static final ExitAction EXIT = new ExitAction();
    /*package*/ static final RunAction RUN = new RunAction();
    /*package*/ static final StopAction STOP = new StopAction();
    /*package*/ static final AstAction AST = new AstAction();
    
    /*package*/ static class ExitAction extends AbstractAction {
        public ExitAction() {
            super("Exit");
        }
        public char getMnemonic() {
            return 'E';
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Controller.getInstance().exit();
        }
    }

    /*package*/ static class RunAction extends AbstractAction {
        public RunAction() {
            super("Run");
            putValue(SMALL_ICON, createIcon("/org/jb/ui/resources/run.png"));
        }
        public char getMnemonic() {
            return 'R';
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Controller.getInstance().runAst();
        }
    }
    
    /*package*/ static class StopAction extends AbstractAction {
        public StopAction() {
            super("Stop");
            putValue(SMALL_ICON, createIcon("/org/jb/ui/resources/stop.png"));
            setEnabled(false);
        }
        public char getMnemonic() {
            return 'S';
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Controller.getInstance().stopForegroundTask();
        }
    }
    
    /*package*/ static class AstAction extends AbstractAction {
        public AstAction() {
            super("Show Ast");
            putValue(SMALL_ICON, createIcon("/org/jb/ui/resources/ast.gif"));
        }
        public char getMnemonic() {
            return 'T';
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Controller.getInstance().showAstInOutputWindow();
        }
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    private static ImageIcon createIcon(String path) {        
        URL url = Actions.class.getResource(path);
        if (url != null) {
            return new ImageIcon(url);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }    
}
