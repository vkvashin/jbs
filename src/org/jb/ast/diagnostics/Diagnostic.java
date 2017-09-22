package org.jb.ast.diagnostics;

/**
 * Represents a diagnostic message.
 * @author vkvashin
 */
public final class Diagnostic {
    
    private enum Level {
        /** Just a warning. */
        WARNING("Warning"),
        
        /** A (syntax) error occurred; the rest of the source can be probably successfully processed. */
        ERROR("Error"),
        
        /** A fatal error occurred; further processing is impossible. */
        FATAL("Fatal error");

        public String getDisplayName() {
            return displayName;
        }
        private final String displayName;
        private Level(String displayName) {
            this.displayName = displayName;
        }
        
    }

    private final int line;
    private final int column;
    private final CharSequence message;
    private final  Level level;
    private Diagnostic chained;
    
    public static Diagnostic warning(int line, int column, CharSequence message) {
        return new Diagnostic(Level.WARNING, line, column, message);
    }

    public static Diagnostic error(int line, int column, CharSequence message) {
        return new Diagnostic(Level.ERROR, line, column, message);
    }

    public static Diagnostic fatal(int line, int column, CharSequence message) {
        return new Diagnostic(Level.FATAL, line, column, message);
    }

    private Diagnostic(Level level, int line, int column, CharSequence message) {
        this.level = level;
        this.line = line;
        this.column = column;
        this.message = message;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public CharSequence getMessage() {
        return message;
    }

    public Diagnostic getChained() {
        return chained;
    }
    
    public void addChained(int line, int column, CharSequence message) {
        Diagnostic toAdd = new Diagnostic(level, line, column, message); // level should be the same as this one
        Diagnostic tail = this;
        while (tail.chained != null) {
            tail = tail.chained;
        }
        tail.chained = toAdd;
    }    

    @Override
    public String toString() {
        return level.toString() + " [" + line + ':' + column + "] " + message;
    }
    
    /** I recommend not to use toString() in UI since in general you never know whether it's for debug/trace purpose */
    public String getDisplayText() {
        return level.getDisplayName() + " at " + line + ':' + column + ": " + message;
    }    
}
