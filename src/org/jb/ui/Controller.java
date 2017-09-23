package org.jb.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jb.ast.api.ASTNode;
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

    private volatile EditorWindow editorWindow;
    private volatile OutputWindow outputWindow;
    
    private final ThreadPoolExecutor foregroundExecutor;
    private final BlockingQueue<Runnable> foregroundQueue;    
    private volatile Future<?> currentTask;

    public Controller() {
        foregroundQueue = new ArrayBlockingQueue(1000);
        foregroundExecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, foregroundQueue);
    }

    public void init(EditorWindow editorWindow, OutputWindow outputWindow) {
        assert this.editorWindow == null;
        assert this.outputWindow == null;
        this.editorWindow = editorWindow;
        this.outputWindow = outputWindow;        
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
        foregroundTaskStarted();
        final String src = editorWindow.getText();
        currentTask = foregroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                showAstInOutputWindowImpl(src);
                foregroundTaskFinished();
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
        } catch (UnsupportedEncodingException ex) {
            outputWindow.printErr(ex.getLocalizedMessage());
        } catch (TokenStreamException ex) {
            outputWindow.printErr(ex.getLocalizedMessage());
        }
    }
    
    public void stopForegroundTask() {
        Future<?> task = currentTask;
        if (task != null) {
            task.cancel(true);
        }
    }
    
    private void foregroundTaskStarted() {
        Runnable r = () -> { Actions.AST.setEnabled(false); Actions.RUN.setEnabled(false); Actions.STOP.setEnabled(true); };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private void foregroundTaskFinished() {
        Runnable r = () -> { Actions.AST.setEnabled(true); Actions.RUN.setEnabled(true); Actions.STOP.setEnabled(false); };
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
                outputWindow.printOut(indentBuffer.toString(), toString(ast).toString());
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
    
    private static void debugSleep() {
        try { Thread.sleep(10000); } catch (InterruptedException ex) {}
    }
}

