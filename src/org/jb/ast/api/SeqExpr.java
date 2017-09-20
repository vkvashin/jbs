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
public final class SeqExpr extends Expr {

    private final Expr first;
    private final Expr last;

    public SeqExpr(int line, int column, Expr first, Expr last) {
        super(line, column);
        this.first = first;
        this.last = last;
        this.first.setNextSibling(this.last);
        this.last.setNextSibling(null);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.SEQ;
    }

    @Override
    public Expr getFirstChild() {
        return first;
    }

    public Expr getFirst() {
        return first;
    }

    public Expr getLast() {
        return last;
    }

    @Override
    public String toString() {
        return super.toString(); // + toString(first, last);
    }
}
