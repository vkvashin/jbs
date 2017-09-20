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
public final class ParenExpr extends Expr {

    private final Expr expr;

    public ParenExpr(int line, int column, Expr expr) {
        super(line, column);
        this.expr = expr;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.PAREN;
    }

    @Override
    public Expr getFirstChild() {
        return expr;
    }

    @Override
    public String toString() {
        return super.toString() + expr;
    }
}
