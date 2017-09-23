package org.jb.ui;

import javax.swing.SwingUtilities;

/**
 *
 * @author vkvashin
 */
public class JbsConsole {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               trySetLAF();
               new MainWindow().setVisible(true);
           } 
        });
    }
    private static void trySetLAF() {
        try {
            javax.swing.UIManager.LookAndFeelInfo[] installedLookAndFeels = javax.swing.UIManager.getInstalledLookAndFeels();
            for (int idx = 0; idx < installedLookAndFeels.length; idx++) {
                if ("Nimbus".equals(installedLookAndFeels[idx].getName())) {
                    javax.swing.UIManager.setLookAndFeel(installedLookAndFeels[idx].getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JbsConsole.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
}
