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
public abstract class Expr extends ASTNode {

    protected Expr(int line, int column) {
        super(line, column);
    }

    public abstract Type getType();

    protected static void chainNodes(ASTNode... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].setNextSibling(i + 1 < nodes.length ? nodes[i + 1] : null);
            }
        }
    }
    protected CharSequence toString(Expr... expressions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expressions.length; i++) {
            Expr e = expressions[i];
            sb.append(i > 0 ? ", " : "").append(e == null ? "null" : e);
        }
        return sb;
    }
}
