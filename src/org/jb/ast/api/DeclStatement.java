package org.jb.ast.api;

/**
 * Represents a declaration:
 * either a declaration statement 
 * or a declaration of lambda parameter
 * @author vkvashin
 */
public final class DeclStatement extends Statement {

    private final CharSequence name;
    private final Expr initializer;
            
    /** @param initExpr can be null if this is a lambda declaration */
    public DeclStatement(int line, int column, CharSequence name, Expr initExpr) {
        super(line, column);
        this.name = name;
        this.initializer = initExpr;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.DECL;
    }

    @Override
    public Expr getFirstChild() {
        return initializer;
    }

    public CharSequence getDelarationName() {
        return name;
    }

    public Expr getInitializer() {
        return initializer;
    }

    @Override
    public String toString() {
        return super.toString() + name;
    }
}
