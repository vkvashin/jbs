package org.jb.ast.api;

/**
 * Represents an AST node
 * @author vkvashin
 */
public abstract class JbNode {
    public enum NodeKind {
        // statements:
        DECL,
        OUT,
        PRINT,
        // expressions 
        PAREN,
        OP,
        ID,
        SEQ,
        MAP,
        REDUCE,
        // literals
        INT,
        FLOAT,
        STRING
    }

    private final int line;
    private final int column;

    protected JbNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public final int getLine() {
        return line;
    }

    public final int getColumn() {
        return column;
    }
    
    public abstract NodeKind getNodeKind();
    public abstract JbNode getFirstChild();
    public abstract JbNode getNextSibling();

    @Override
    public String toString() {
        return getNodeKind().toString() + " [" + line + ':' + column + "] ";
    }
}
