package org.jb.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.jb.ast.api.ASTNode;
import org.jb.ast.diagnostics.DefaultDiagnosticListener;
import org.jb.evaluator.api.Evaluator;
import org.jb.lexer.api.Lexer;
import org.jb.lexer.api.TokenStream;
import org.jb.lexer.api.TokenStreamException;
import org.jb.parser.api.Parser;

/**
 *
 * @author vkvashin
 */
public class JbsConsole {
    public static void main(String[] args) {
        if (args.length > 0) {
            processCommandLine(args);
        } else {
            SwingUtilities.invokeLater(() -> {
                trySetLAF();
                MainWindow mainWindow = new MainWindow();
                mainWindow.initComponents();
                mainWindow.setVisible(true);
            });
        }
    }

    private enum State {
        FILE,
        COMMAND,
        NONE
    }

    private static void usage() {
        System.err.println("Usage: java -jar jbs.jar [-p] -c <program> | -f <file> ");
        System.err.println("where");
        System.err.println("-p allows parallel evaluation of the single progrem");
        System.err.println("-f means that the program is in the <file>");
        System.err.println("-c allows to specify program on the command line");
    }

    private static void processCommandLine(String[] args) {
        assert args.length > 0;
        boolean allowParallel = false;
        List<File> files = new ArrayList<>();
        List<String> programs = new ArrayList<>();
        State state = State.NONE;
        for (String arg : args) {
            if ("-f".equals(arg)) {
                state = State.FILE;
            } else if ("-c".equals(arg)) {
                state = State.COMMAND;
            } else if ("-p".equals(arg)) {
                allowParallel = true;
                state = State.NONE;
            } else if ("-?".equals(arg) || "-h".equals(arg) || "-help".equals(arg) || "--help".equals(arg)) {
                usage();
                return;
            } else if (arg.startsWith("-")) {
                // nothing - we assume it's a java flag
            } else {
                switch (state) {
                    case FILE:
                        files.add(new File(arg));
                        break;
                    case COMMAND:
                        programs.add(arg);
                        break;
                    case NONE:
                        usage();
                        return;
                    default:
                        throw new AssertionError(state.name());
                }
            }
        }

        for (String program : programs) {
            try {
                run(new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8.name())), allowParallel);
            } catch (UnsupportedEncodingException ex) {
                System.err.println(ex.getLocalizedMessage());
            }            
        }
        for (File file : files) {
            try {
                run(new FileInputStream(file), allowParallel);
            } catch (FileNotFoundException ex) {
                System.err.println("Can not open " + file.getPath() + ": " + ex.getLocalizedMessage());
            }
        }
    }

    private static void run(InputStream is, boolean allowParallel) {
        if (is == null) {
            return;
        }
        Lexer lexer = new Lexer(is);
        TokenStream ts = lexer.lex();
        Parser parser = new Parser();
        ASTNode ast;
        try {
            ast = parser.parse(ts);
            Evaluator evaluator = new Evaluator(System.out, allowParallel, DefaultDiagnosticListener.getDefaultListener());
            long time = System.currentTimeMillis();
            evaluator.execute(ast);
            time = System.currentTimeMillis() - time;
            evaluator.dispose();
            System.err.println("[evaluation took " + time + " ms]");
        } catch (TokenStreamException ex) {
            System.err.println(ex.getLocalizedMessage());
        }
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
