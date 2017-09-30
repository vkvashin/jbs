package org.jb.ui;

import java.awt.event.*;
import java.net.URL;
import javax.swing.*;
import static javax.swing.Action.SMALL_ICON;

/**
 *
 * @author vkvashin
 */
public class Actions {

    public static class ActionEx extends AbstractAction {
        private final char mnemonic;
        private final Runnable worker;
        public ActionEx(String name, char mnemonic, Runnable worker) {
            super(name);
            this.mnemonic = mnemonic;
            this.worker = worker;
        }
        public ActionEx(String name, char mnemonic, Runnable worker, Icon icon) {
            super(name, icon);
            this.mnemonic = mnemonic;
            this.worker = worker;
            putValue(SMALL_ICON, icon);
        }
        public char getMnemonic() {
            return mnemonic;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            worker.run();
        }
    }

    public static class CheckboxAction extends ActionEx {
        private final Predicate predicate;
        public CheckboxAction(String name, char mnemonic, Runnable worker, Predicate predicate) {
            super(name, mnemonic, worker);
            this.predicate = predicate;
        }
        public boolean isChecked() {
            return predicate.test();
        }
    }

    @FunctionalInterface
    private interface Predicate {
        boolean test();
    }

    /*package*/ static final ActionEx OPEN = new ActionEx("Open", 'o', () -> Controller.getInstance().open());
    /*package*/ static final ActionEx SAVE = new ActionEx("Save", 's', () -> Controller.getInstance().save());
    /*package*/ static final ActionEx SAVE_AS = new ActionEx("Save As", 'a', () -> Controller.getInstance().saveAs());
    /*package*/ static final ActionEx EXIT = new ActionEx("Exit", 'e', () -> Controller.getInstance().exit());

    /*package*/ static final ActionEx RUN = new ActionEx("Run", 'r', () -> Controller.getInstance().runAst(), createIcon("/org/jb/ui/resources/run.png"));
    /*package*/ static final ActionEx STOP = new ActionEx("Stop", 's', () -> Controller.getInstance().stop(), createIcon("/org/jb/ui/resources/stop.png"));
    /*package*/ static final ActionEx AST = new ActionEx("Show Ast", 'a', ()-> Controller.getInstance().showAstInOutputWindow(), createIcon("/org/jb/ui/resources/ast.gif"));

    /*package*/ static final CheckboxAction AUTORUN = new CheckboxAction(
            "Autorun", 'A',
            () -> {
                Controller.getInstance().toggleAutorun();
                if (Controller.getInstance().isAutorun()) {
                    RUN.setEnabled(false);
                }
            },
            () -> Controller.getInstance().isAutorun());

    /*package*/ static final CheckboxAction PROCEED_ON_ERROR = new CheckboxAction(
            "Proceed on error", 'e',
            () -> Controller.getInstance().toggleProceedOnError(),
            () -> Controller.getInstance().isProceedOnError());

    /*package*/ static final CheckboxAction ALLOW_PARALLELIZATION = new CheckboxAction(
            "Allow Parallelisation", 'p',
            () -> Controller.getInstance().toggleParallelisation(),
            () -> Controller.getInstance().isParallelisationAllowed());

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
