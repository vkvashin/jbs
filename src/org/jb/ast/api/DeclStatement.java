/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.ast.api;

/**
 *
 * @author vkvashin
 */
public final class DeclStatement extends Statement {

    private final CharSequence name;
    private final Expr initializer;
            
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

    public CharSequence getName() {
        return name;
    }

    public Expr getInitializer() {
        return initializer;
    }
}
