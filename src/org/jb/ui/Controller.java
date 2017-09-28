package org.jb.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
    
    private final ThreadPoolExecutor explicitTaskExecutor;
    private final BlockingQueue<Runnable> explicitTaskQueue;
    private volatile Future<?> currentExplicitTask;
    
    private final ThreadPoolExecutor autoTasksExecutor;
    private final BlockingQueue<Runnable> autoTasksQueue;
    private volatile Future<?> currentAutoTask;

    private volatile boolean autorun = true;
    private volatile boolean proceedOnError = false;

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
        explicitTaskQueue = new ArrayBlockingQueue(1000);
        explicitTaskExecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, explicitTaskQueue);
        autoTasksQueue = new ArrayBlockingQueue(1000);
        autoTasksExecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, autoTasksQueue);
        docUpdateTimer = new javax.swing.Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assert SwingUtilities.isEventDispatchThread();
                scheduleSyntaxCheckOrRun(editorWindow.getText(), ++updateId);
            }
        });
    }

    public void init(EditorWindow editorWindow, OutputWindow outputWindow) {
        assert this.editorWindow == null;
        assert this.outputWindow == null;
        this.editorWindow = editorWindow;
        this.outputWindow = outputWindow;
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
        explicitTaskStarted();
        final String src = editorWindow.getText();
        currentExplicitTask = explicitTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                showAstInOutputWindowImpl(src);
                explicitTaskFinished();
            }
        });
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
        explicitTaskStarted();
        final String src = editorWindow.getText();
        currentExplicitTask = explicitTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    runAstImpl(src);
                } finally {
                    explicitTaskFinished();
                }
            }
        });
    }

    private void runAstImpl(String src) {
        try {
            InputStream is = getInputStream(src);
            Lexer lexer = new Lexer(is, outputWindow.getDiagnosticListener());
            TokenStream ts = lexer.lex();
            Parser parser = new Parser();
            ASTNode ast = parser.parse(ts, outputWindow.getDiagnosticListener());
            Evaluator evaluator = new Evaluator(
                    outputWindow.getOutputAsAppendable(), outputWindow.getDiagnosticListener());
            evaluator.execute(ast);
        } catch (UnsupportedEncodingException | TokenStreamException ex) {
            outputWindow.printErr(ex.getLocalizedMessage());
        } catch (OutOfMemoryError ex) {
            outputWindow.printErr("Insufficient memory to complete the operation");
        } catch (Throwable ex) {
            outputWindow.printErr("Unexpected error: " + ex.getLocalizedMessage());
        }
    }

    /** 
     * Schedules a syntax check of text in editor.
     * If previous check has been scheduled, it will be canceled.
     */
    private void scheduleSyntaxCheckOrRun(final String source, final int updateId) {
        Future<?> prev = currentAutoTask;
        if (prev != null) {
            prev.cancel(true);
        }
        currentAutoTask = autoTasksExecutor.submit(new Runnable() {
            @Override
            public void run() {
                syntaxCheckOrRun(source, updateId);
            }
        });
    }

    private void syntaxCheckOrRun(String source, final int updateId) {
        final List<Diagnostic> diagnostics = new ArrayList<>();
        DiagnosticListener myListener = new DiagnosticListener() {
            @Override
            public void report(Diagnostic issue) {
                diagnostics.add(issue);
            }
        };
        DiagnosticListener[] listeners = autorun ?
                new DiagnosticListener[] {myListener, outputWindow.getDiagnosticListener()} :
                new DiagnosticListener[] {myListener};
        try {
            if (autorun) {
                outputWindow.clear();                
            }
            InputStream is = getInputStream(source);
            Lexer lexer = new Lexer(is, listeners);
            TokenStream ts = lexer.lex();
            Parser parser = new Parser();
            ASTNode ast = parser.parse(ts, listeners);
            if (autorun && (diagnostics.isEmpty() || proceedOnError)) {
                SwingUtilities.invokeLater(() -> Actions.STOP.setEnabled(true));
                try {
                    Evaluator evaluator = new Evaluator(
                            outputWindow.getOutputAsAppendable(), listeners);
                    evaluator.execute(ast);
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
        underlineErrors(diagnostics, updateId);
    }

    private void underlineErrors(List<Diagnostic> diagnostics, final int updateId) {
        if (SwingUtilities.isEventDispatchThread()) {
            underlineErrorsImpl(diagnostics, updateId);
        } else {
            SwingUtilities.invokeLater(() -> underlineErrorsImpl(diagnostics, updateId));
        }
    }

    private void underlineErrorsImpl(List<Diagnostic> diagnostics, final int updateId) {
        assert SwingUtilities.isEventDispatchThread();
        if (updateId != this.updateId) {
            return;
        }
        editorWindow.underlineErrors(diagnostics);
    }

    public void stop() {
        Future<?> task = currentExplicitTask;
        if (task != null) {
            task.cancel(true);
        }
        if (autorun) {
            task = currentAutoTask;
            if (task != null) {
                task.cancel(true);
            }
        }
    }

    private void explicitTaskStarted() {
        Runnable r = () -> {
            Actions.AST.setEnabled(false);
            Actions.RUN.setEnabled(false);
            Actions.STOP.setEnabled(true);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private void explicitTaskFinished() {
        Runnable r = () -> { 
            Actions.AST.setEnabled(true);
            Actions.RUN.setEnabled(!isAutorun());
            Actions.STOP.setEnabled(false);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
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
    }

    public boolean isProceedOnError() {
        return proceedOnError;
    }

    public void toggleProceedOnError() {
        proceedOnError = ! proceedOnError;
    }

    private static void debugSleep() {
        try { Thread.sleep(10000); } catch (InterruptedException ex) {}
    }
}

