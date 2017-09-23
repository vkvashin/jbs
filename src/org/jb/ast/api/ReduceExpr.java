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
    private final Expr defValue;
    private final DeclStatement prev;
    private final DeclStatement curr;
    private final Expr transformation;

    public ReduceExpr(int line, int column, Expr sequence, Expr defValue, DeclStatement prev, DeclStatement curr, Expr transformation) {
        super(line, column);
        this.sequence = sequence;
        this.defValue = defValue;
        this.prev = prev;
        this.curr = curr;
        this.transformation = transformation;
        chainNodes(this.sequence, this.defValue, this.prev, this.curr, this.transformation);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.REDUCE;
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
        return super.toString(); // + toString(defValue, prev, curr, transformation);
    }

    @Override
    public Type getType() {
        return transformation.getType();
    }    
}
