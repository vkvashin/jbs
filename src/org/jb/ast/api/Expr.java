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

    private Expr nextExpr;

    protected Expr(int line, int column) {
        super(line, column);
    }

    public final void setNextSibling(Expr nextSibling) {
        this.nextExpr = nextSibling;
    }

    @Override
    public final ASTNode getNextSibling() {
        return nextExpr;
    }

    protected static void chainExpressions(Expr... exps) {
        for (int i = 0; i < exps.length; i++) {
            if (exps[i] != null) {
                exps[i].setNextSibling(i + 1 < exps.length ? exps[i + 1] : null);
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
