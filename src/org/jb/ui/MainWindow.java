package org.jb.ui;

import java.awt.*;
import java.io.File;
import javax.swing.*;

/**
 *
 * @author vkvashin
 */
/*package*/ class MainWindow extends JFrame {

    private final JSplitPane splitPane;
    private final EditorWindow editorWindow;
    private final OutputWindow outputWindow;
    private final JMenuBar mainMenu;

    private static final String TITLE = "JbScript console";

    public MainWindow() {
        super(TITLE);
        this.editorWindow = new EditorWindow();
        this.outputWindow = new OutputWindow();
        mainMenu = initMenu();
        this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    }

    public void fileChanged(File file) {
        setTitle(TITLE + " [" + file.getName() + ']');
    }

    public void initComponents() {
        Controller.getInstance().init(editorWindow, outputWindow, this);
        splitPane.setLeftComponent(editorWindow);
        splitPane.setRightComponent(outputWindow);
        //splitPane.setDividerLocation(0.66);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        setJMenuBar(mainMenu);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                Controller.getInstance().exit();
            }
        });
        adjustSizeAndPosition();
    }
    
    private void adjustSizeAndPosition() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = new Dimension(screenSize.width / 2, screenSize.height / 2);
        editorWindow.setPreferredSize(new Dimension(editorWindow.getPreferredSize().width, frameSize.height / 2));
        setSize(frameSize);
        setLocation(new Point((screenSize.width - frameSize.width) / 2,
                              (screenSize.height - frameSize.height) / 2));        
    }

    private static void addMenuItem(JMenu menu, Actions.ActionEx action) {
        JMenuItem item = new JMenuItem(action);
        item.setMnemonic(action.getMnemonic());
        menu.add(item);
    }

    private static void addCheckboxMenuItem(JMenu menu, Actions.CheckboxAction action) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
        item.setMnemonic(action.getMnemonic());
        item.setState(action.isChecked());
        menu.add(item);
    }

    private JMenuBar initMenu() {
        JMenuBar menu = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        addMenuItem(fileMenu, Actions.OPEN);
        addMenuItem(fileMenu, Actions.SAVE);
        addMenuItem(fileMenu, Actions.SAVE_AS);
        addMenuItem(fileMenu, Actions.EXIT);
        menu.add(fileMenu);
        
        JMenu runMenu = new JMenu("Run");
        runMenu.setMnemonic('R');
        addMenuItem(runMenu, Actions.RUN);
        addMenuItem(runMenu, Actions.AST);
        addMenuItem(runMenu, Actions.STOP);
        menu.add(runMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('O');
        addCheckboxMenuItem(optionsMenu, Actions.AUTORUN);
        addCheckboxMenuItem(optionsMenu, Actions.PROCEED_ON_ERROR);
        addCheckboxMenuItem(optionsMenu, Actions.ALLOW_PARALLELIZATION);
        menu.add(optionsMenu);

        return menu;
    }
}
