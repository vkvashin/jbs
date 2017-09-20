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
public final class BinaryOpExpr extends Expr {

    public enum OpKind {
        ADD('+'),
        SUB('-'),
        DIV('/'),
        MUL('*'),
        POW('^');
        public final char id;
        private OpKind(char id) {
            this.id = id;
        }
    }

    private final OpKind opKind;
    private final Expr left;
    private final Expr right;

    public BinaryOpExpr(int line, int column, OpKind kind, Expr left, Expr right) {
        super(line, column);
        this.opKind = kind;
        this.left = left;
        this.right = right;
        this.left.setNextSibling(this.right);
        this.right.setNextSibling(null);
    }

    @Override
    public final NodeKind getNodeKind() {
        return NodeKind.OP;
    }

    public final OpKind getOpKind() {
        return opKind;
    }

    @Override
    public Expr getFirstChild() {
        return left;
    }

    public final Expr getLeft() {
        return left;
    }

    public final Expr getRight() {
        return right;
    }

    @Override
    public String toString() {
        return super.toString() + opKind.id + ' ' + toString(left, right);
    }
}
