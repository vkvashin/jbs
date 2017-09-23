package org.jb.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

/**
 *
 * @author vkvashin
 */
/*package*/ class MainWindow extends JFrame {

    private final JSplitPane splitPane;
    private final EditorWindow editorWindow;
    private final OutputWindow outputWindow;
    private final JMenuBar mainMenu;

    public MainWindow() {
        super("JbScript console");
        this.editorWindow = new EditorWindow();
        this.outputWindow = new OutputWindow();
        Controller.getInstance().init(editorWindow, outputWindow);
        this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setLeftComponent(editorWindow);
        splitPane.setRightComponent(outputWindow);        
        //splitPane.setDividerLocation(0.66);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        mainMenu = initMenu();
        setJMenuBar(mainMenu);
        addWindowListener(new java.awt.event.WindowAdapter() {
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

        JMenuItem aboutItem = new JMenuItem(Actions.ABOUT);
        aboutItem.setMnemonic(Actions.ABOUT.getMnemonic());
        fileMenu.add(aboutItem);
        
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

        return menu;
    } 
}
