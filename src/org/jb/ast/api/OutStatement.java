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
public final class OutStatement extends Statement {

    private final Expr expr;
    
    public OutStatement(int line, int column, Expr expr) {
        super(line, column);
        this.expr = expr;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.OUT;
    }    
    
    @Override
    public Expr getFirstChild() {
        return expr;
    }

    public Expr getExpr() {
        return expr;
    }
}
