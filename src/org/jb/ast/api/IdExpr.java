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
public final class IdExpr extends Expr {

    private final CharSequence name;

    public IdExpr(CharSequence name, int line, int column) {
        super(line, column);
        this.name = name;
    }

    public CharSequence getName() {
        return name;
    }
    
    @Override
    public NodeKind getNodeKind() {
        return NodeKind.ID;
    }

    @Override
    public ASTNode getFirstChild() {
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + name;
    }
}
