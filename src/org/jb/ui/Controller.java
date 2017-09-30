package org.jb.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.HTMLEditorKit;
import org.jb.ast.api.ASTNode;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.ast.diagnostics.DiagnosticListener;
import org.jb.evaluator.api.Evaluator;
import org.jb.lexer.api.Lexer;
import org.jb.lexer.api.TokenStream;
import org.jb.lexer.api.TokenStreamException;
import org.jb.parser.api.Parser;

/**
 *
 * @author vkvashin
 */
/*package*/ class Controller {
    
    
    private static final Controller INSTANCE = new Controller();

    public static final boolean TRACE = Boolean.getBoolean("jbs.trace");
    
    private volatile EditorWindow editorWindow;
    private volatile OutputWindow outputWindow;
    private volatile MainWindow mainWindow;

    // When autorun is OFF, calculations are launched explicitly.
    // In this mode, if a calculation takes long, user can edit text,
    // and get syntax errors on the fly - and all these should not stop calculation.
    // That's why we need 2 thread pools and 2 current tasks.
    // In automatic mode, it everything is more simple

    // If autorun is ON, only this pair is used;
    // if autorun is OFF, it is used for explicit actions
    private final ThreadPoolExecutor longTaskExecutor;
    private volatile Future<?> currentLongTask;

    // If autorun is ON, it is never used;
    // if it is OFF, used for automatic symtax check
    private final ThreadPoolExecutor autoLightWeightTaskExecutor;
    private volatile Future<?> currentLightWeightAutoTask;

    private volatile boolean autorun = true;
    private volatile boolean proceedOnError = false;
    private volatile boolean allowParallelisation = true;

    private File lastDirectory = null;
    private File currentFile = null;

    /**
     * Incremented each time we schedule a syntax check.
     * When the check is done in a separate thread, and we switch to EDT to display errors,
     * we check whether this number is still the same. If it is not, we won't show errors -
     * they are already outdated.
     *
     * Should be accessed from EDT only!
     */
    private int updateId = 0;
    private final javax.swing.Timer docUpdateTimer;

    public Controller() {
        longTaskExecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue(1000));
        autoLightWeightTaskExecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue(1000));
        docUpdateTimer = new javax.swing.Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assert SwingUtilities.isEventDispatchThread();
                if (autorun) {
                    cancelLongTask();
                    submitLongTask(() -> syntaxCheckOrRun(editorWindow.getText(), updateId, true));
                } else {
                    cancelLightweightTask();
                    submitLightweightTask(() -> syntaxCheckOrRun(editorWindow.getText(), updateId, false));

                }
            }
        });
    }

    public void init(EditorWindow editorWindow, OutputWindow outputWindow, MainWindow mainWindow) {
        assert this.editorWindow == null;
        assert this.outputWindow == null;
        this.editorWindow = editorWindow;
        this.outputWindow = outputWindow;
        this.mainWindow = mainWindow;
        docUpdateTimer.setRepeats(false);
        editorWindow.addDocumentListener(new DocumentListener() {
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

            private void documentUpdated() {
                // Here we use javax.swing.Timer instead of java.util.concurrency staff
                // because javax.swing.Timer raises its event in EDT.
                // This allows us not to safely make editor content copy
                // and at the same time to do this only when necessary.
                docUpdateTimer.stop();
                docUpdateTimer.start();
            }
        });
        Actions.RUN.setEnabled(!isAutorun());
    }

    public static Controller getInstance() {
        return INSTANCE;
    }

    public void exit() {
        System.exit(0);
    }

    /**
     * asynchronously parses the text from editor and dumps its AST into output window
     */
    public void showAstInOutputWindow() {
        assert SwingUtilities.isEventDispatchThread();
        outputWindow.clear();
        final String src = editorWindow.getText();
        // the task isn't actually long, but it is explicit => use longTaskExecutor
        cancelLongTask();
        submitLongTask(() -> showAstInOutputWindowImpl(src));
    }
    
    private void showAstInOutputWindowImpl(String src) {
        try {
            InputStream is = getInputStream(src);
            Lexer lexer = new Lexer(is, outputWindow.getDiagnosticListener());
            TokenStream ts = lexer.lex();
            Parser parser = new Parser();
            ASTNode ast = parser.parse(ts, outputWindow.getDiagnosticListener());
            AstPrinter printer = new AstPrinter();            
            printer.printAst(ast);
        } catch (UnsupportedEncodingException | TokenStreamException ex) {
            outputWindow.printErr(ex.getLocalizedMessage());
        } catch (Throwable ex) {
            outputWindow.printErr("Unexpected error: " + ex.getLocalizedMessage());
        }
    }

    /**
     * asynchronously parses the text from editor and interprets AST
     */
    public void runAst() {
        assert SwingUtilities.isEventDispatchThread();
        outputWindow.clear();
        final String src = editorWindow.getText();
        submitLongTask(() -> runAstImpl(src));
    }

    private void runAstImpl(String src) {
        try {
            InputStream is = getInputStream(src);
            Lexer lexer = new Lexer(is, outputWindow.getDiagnosticListener());
            TokenStream ts = lexer.lex();
            Parser parser = new Parser();
            ASTNode ast = parser.parse(ts, outputWindow.getDiagnosticListener());
            Evaluator evaluator = new Evaluator(
                    outputWindow.getOutputAsAppendable(), allowParallelisation, outputWindow.getDiagnosticListener());
            evaluator.execute(ast);
            evaluator.dispose();
        } catch (UnsupportedEncodingException | TokenStreamException ex) {
            outputWindow.printErr(ex.getLocalizedMessage());
        } catch (OutOfMemoryError ex) {
            outputWindow.printErr("Insufficient memory to complete the operation");
        } catch (Throwable ex) {
            outputWindow.printErr("Unexpected error: " + ex.getLocalizedMessage());
        }
    }

    private void syntaxCheckOrRun(String source, final int updateId, boolean run) {
        final List<Diagnostic> diagnostics = new ArrayList<>();
        DiagnosticListener myListener = new DiagnosticListener() {
            @Override
            public void report(Diagnostic issue) {
                diagnostics.add(issue);
            }
        };
        DiagnosticListener[] listeners = run ?
                new DiagnosticListener[] {myListener, outputWindow.getDiagnosticListener()} :
                new DiagnosticListener[] {myListener};
        try {
            if (run) {
                outputWindow.clear();                
            }
            InputStream is = getInputStream(source);
            Lexer lexer = new Lexer(is, listeners);
            TokenStream ts = lexer.lex();
            Parser parser = new Parser();
            ASTNode ast = parser.parse(ts, listeners);
            if (run && (diagnostics.isEmpty() || proceedOnError)) {
                SwingUtilities.invokeLater(() -> Actions.STOP.setEnabled(true));
                try {
                    Evaluator evaluator = new Evaluator(
                            outputWindow.getOutputAsAppendable(), allowParallelisation, listeners);
                    evaluator.execute(ast);
                    evaluator.dispose();
                } catch (OutOfMemoryError ex) {
                    outputWindow.printErr("Insufficient memory to complete the operation");
                } catch (Throwable ex) {
                    outputWindow.printErr("Unexpected error: " + ex.getLocalizedMessage());
                } finally {
                    SwingUtilities.invokeLater(() -> Actions.STOP.setEnabled(false));
                }
            }
        } catch (UnsupportedEncodingException ex) {
            outputWindow.printErr(ex.getLocalizedMessage());
        } catch (TokenStreamException ex) {
            outputWindow.printErr(ex.getLocalizedMessage());
        }
        if (TRACE) {
            System.out.println("--- error check done ---");
            for (Diagnostic d : diagnostics) {
                System.out.println(d.toString());
            }
        }
        runInEDT(() -> underlineErrors(diagnostics, updateId));
    }

    private void underlineErrors(List<Diagnostic> diagnostics, final int updateId) {
        assert SwingUtilities.isEventDispatchThread();
        if (updateId != this.updateId) {
            return;
        }
        editorWindow.underlineErrors(diagnostics);
    }

    public void stop() {
        cancelLongTask();
    }

    private void runInEDT(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private void submitLongTask(Runnable task) {
        runInEDT(() -> {
            Actions.AST.setEnabled(false);
            Actions.RUN.setEnabled(false);
            Actions.STOP.setEnabled(true);
        });
        currentLongTask = longTaskExecutor.submit(() -> {
            try {
                task.run();
            } finally {
                runInEDT(() -> {
                    Actions.AST.setEnabled(true);
                    Actions.RUN.setEnabled(!isAutorun());
                    Actions.STOP.setEnabled(false);
                });
            }
        });
    }

    private void cancelLongTask() {
        Future<?> task = currentLongTask;
        if (task != null) {
            task.cancel(true);
        }
    }

    private void submitLightweightTask(Runnable r) {
        currentLightWeightAutoTask = autoLightWeightTaskExecutor.submit(r);
    }

    private void cancelLightweightTask() {
        Future<?> task = currentLightWeightAutoTask;
        if (task != null) {
            task.cancel(true);
        }
    }

    private InputStream getInputStream(String text) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name()));
    }
    
    private class AstPrinter {

        private final int step = 4;
        private final StringBuilder indentBuffer = new StringBuilder();

        private void indent() {
            setupIndentBuffer(indentBuffer.length() + step);
        }

        public void unindent() {
            setupIndentBuffer(indentBuffer.length() - step);
        }

        private void setupIndentBuffer(int len) {
            if (len <= 0) {
                indentBuffer.setLength(0);
            } else {
                indentBuffer.setLength(len);
                for (int i = 0; i < len; i++) {
                    indentBuffer.setCharAt(i, ' ');
                }
            }
        }

        public void printAst(ASTNode ast) {
            while (ast != null) {
                outputWindow.printOut(indentBuffer.toString(), toString(ast).toString(), "\n");
                ASTNode firstChild = ast.getFirstChild();
                if (firstChild != null) {
                    indent();
                    printAst(firstChild);
                    unindent();
                }
                ast = ast.getNextSibling();
            }
        }

        private CharSequence toString(ASTNode ast) {
            return ast.toString();
        }
    }    

    public boolean isAutorun() {
        return autorun;
    }

    public void toggleAutorun() {
        autorun = ! autorun;
        Actions.RUN.setEnabled(!autorun);
    }

    public boolean isProceedOnError() {
        return proceedOnError;
    }

    public void toggleProceedOnError() {
        proceedOnError = ! proceedOnError;
    }

    public boolean isParallelisationAllowed() {
        return allowParallelisation;
    }

    public void toggleParallelisation() {
        allowParallelisation = ! allowParallelisation;
    }

    public void open() {
        assert SwingUtilities.isEventDispatchThread();
        JFileChooser fileChooser = new JFileChooser(lastDirectory);
        int res = fileChooser.showOpenDialog(mainWindow);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            if (file.exists()) {
                try {
                    // TODO: move file operations out from EDT!
                    String newText = new String(Files.readAllBytes(Paths.get(file.toURI())));
                    editorWindow.setText(newText);
                    currentFile = file;
                    mainWindow.fileChanged(currentFile);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainWindow, "Error opening file: " + ex.getLocalizedMessage());
                }
            }
        }
    }

    public void save() {
        assert SwingUtilities.isEventDispatchThread();
        if (currentFile == null) {
            saveAs();
            return;
        }
        saveImpl(currentFile); // TODO: move file operations out from EDT!
    }

    public void saveAs() {
        assert SwingUtilities.isEventDispatchThread();
        JFileChooser fileChooser = new JFileChooser(lastDirectory);
        int res = fileChooser.showSaveDialog(mainWindow);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            if (saveImpl(file)) {
                currentFile = file;
                mainWindow.fileChanged(currentFile);
            }
        }
    }

    private boolean saveImpl(File file) {
        final String text = editorWindow.getText();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // TODO: move file operations out from EDT!
            writer.write(text);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(mainWindow, "Error saving file: " + ex.getLocalizedMessage());
            return false;
        }
    }

    private static void debugSleep() {
        try { Thread.sleep(10000); } catch (InterruptedException ex) {}
    }
}

