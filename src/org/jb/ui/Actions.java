package org.jb.ui;

import java.awt.event.*;
import java.awt.*;
import java.net.URL;
import javax.swing.*;
import static javax.swing.Action.SMALL_ICON;

/**
 *
 * @author vkvashin
 */
public class Actions {
    
    /*package*/ static final ExitAction EXIT = new ExitAction();
    /*package*/ static final RunAction RUN = new RunAction();
    /*package*/ static final StopAction STOP = new StopAction();
    /*package*/ static final AstAction AST = new AstAction();
    /*package*/ static final AutorunAction AUTORUN = new AutorunAction();
    /*package*/ static final ProceedOnError PROCEED_ON_ERROR = new ProceedOnError();
    /*package*/ static final AllowParallelisation ALLOW_PARALLELIZATION = new AllowParallelisation();

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
            Controller.getInstance().stop();
        }
    }
    
    /*package*/ static class AstAction extends AbstractAction {
        public AstAction() {
            super("Show Ast");
            putValue(SMALL_ICON, createIcon("/org/jb/ui/resources/ast.gif"));
        }
        public char getMnemonic() {
            return 'A';
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Controller.getInstance().showAstInOutputWindow();
        }
    }

    /*package*/ static class AutorunAction extends AbstractAction {
        public AutorunAction() {
            super("Autorun");
        }
        public char getMnemonic() {
            return 'A';
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Controller.getInstance().toggleAutorun();
            if (Controller.getInstance().isAutorun()) {
                RUN.setEnabled(false);
            }
        }
        public boolean isChecked() {
            return Controller.getInstance().isAutorun();
        }
    }

    /*package*/ static class ProceedOnError extends AbstractAction {
        public ProceedOnError() {
            super("Proceed on error");
        }
        public char getMnemonic() {
            return 'e';
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Controller.getInstance().toggleProceedOnError();
        }
        public boolean isChecked() {
            return Controller.getInstance().isProceedOnError();
        }
    }

    /*package*/ static class AllowParallelisation extends AbstractAction {
        public AllowParallelisation() {
            super("Allow Parallelisation");
        }
        public char getMnemonic() {
            return 'p';
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Controller.getInstance().toggleParallelisation();
        }
        public boolean isChecked() {
            return Controller.getInstance().isParallelisationAllowed();
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
