package org.jb.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author vkvashin
 */
public class JbsConsole {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            trySetLAF();
            MainWindow mainWindow = new MainWindow();
            mainWindow.initComponents();
            mainWindow.setVisible(true);
        });
    }
    private static void trySetLAF() {
        try {
            javax.swing.UIManager.LookAndFeelInfo[] installedLookAndFeels = javax.swing.UIManager.getInstalledLookAndFeels();
            for (UIManager.LookAndFeelInfo installedLookAndFeel : installedLookAndFeels) {
                if ("Nimbus".equals(installedLookAndFeel.getName())) {
                    javax.swing.UIManager.setLookAndFeel(installedLookAndFeel.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JbsConsole.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
}
