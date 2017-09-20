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
public final class  ReduceExpr extends Expr {

    private final Expr sequence;
    private final IdExpr defValue;
    private final IdExpr prev;
    private final IdExpr curr;
    private final Expr transformation;

    public ReduceExpr(int line, int column, Expr sequence, IdExpr defValue, IdExpr prev, IdExpr curr, Expr transformation) {
        super(line, column);
        this.sequence = sequence;
        this.defValue = defValue;
        this.prev = prev;
        this.curr = curr;
        this.transformation = transformation;
        chainExpressions(this.sequence, this.defValue, this.prev, this.curr, this.transformation);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.MAP;
    }

    @Override
    public ASTNode getFirstChild() {
        return sequence;
    }

    public Expr getTransformation() {
        return transformation;
    }

    @Override
    public String toString() {
        return super.toString() + toString(defValue, prev, curr, transformation);
    }
}
