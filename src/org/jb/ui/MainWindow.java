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

    private JMenuBar initMenu() {
        JMenuBar menu = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        {
            JMenuItem openItem = new JMenuItem(Actions.OPEN);
            openItem.setMnemonic(Actions.OPEN.getMnemonic());
            fileMenu.add(openItem);
        }

        {
            JMenuItem saveItem = new JMenuItem(Actions.SAVE);
            saveItem.setMnemonic(Actions.SAVE.getMnemonic());
            fileMenu.add(saveItem);
        }

        {
            JMenuItem saveAsItem = new JMenuItem(Actions.SAVE_AS);
            saveAsItem.setMnemonic(Actions.SAVE_AS.getMnemonic());
            fileMenu.add(saveAsItem);
        }

        JMenuItem exitItem = new JMenuItem(Actions.EXIT);
        exitItem.setMnemonic(Actions.EXIT.getMnemonic());
        fileMenu.add(exitItem);
            
        menu.add(fileMenu);
        
        JMenu runMenu = new JMenu("Run");
        runMenu.setMnemonic('R');
        
        JMenuItem runItem = new JMenuItem(Actions.RUN);
        runItem.setMnemonic(Actions.RUN.getMnemonic());
        runMenu.add(runItem);

        JMenuItem astItem = new JMenuItem(Actions.AST);
        astItem.setMnemonic(Actions.AST.getMnemonic());
        runMenu.add(astItem);

        JMenuItem stopItem = new JMenuItem(Actions.STOP);
        stopItem.setMnemonic(Actions.STOP.getMnemonic());
        runMenu.add(stopItem);

        menu.add(runMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('O');

        JCheckBoxMenuItem autorunItem = new JCheckBoxMenuItem(Actions.AUTORUN);
        autorunItem.setMnemonic(Actions.AUTORUN.getMnemonic());
        autorunItem.setState(Actions.AUTORUN.isChecked());
        optionsMenu.add(autorunItem);

        JCheckBoxMenuItem proceedOnErrorItem = new JCheckBoxMenuItem(Actions.PROCEED_ON_ERROR);
        proceedOnErrorItem.setMnemonic(Actions.PROCEED_ON_ERROR.getMnemonic());
        proceedOnErrorItem.setState(Actions.PROCEED_ON_ERROR.isChecked());
        optionsMenu.add(proceedOnErrorItem);

        JCheckBoxMenuItem allowParallelisation = new JCheckBoxMenuItem(Actions.ALLOW_PARALLELIZATION);
        allowParallelisation.setMnemonic(Actions.ALLOW_PARALLELIZATION.getMnemonic());
        allowParallelisation.setState(Actions.ALLOW_PARALLELIZATION.isChecked());
        optionsMenu.add(allowParallelisation);

        menu.add(optionsMenu);

        return menu;
    }
}
