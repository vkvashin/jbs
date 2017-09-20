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
public final class  MapExpr extends Expr {

    private final Expr sequence;
    private final IdExpr var;
    private final Expr transformation;

    public MapExpr(int line, int column, Expr sequence, IdExpr var, Expr transformation) {
        super(line, column);
        this.sequence = sequence;
        this.var = var;
        this.transformation = transformation;
        chainExpressions(this.sequence, this.var, this.transformation);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.MAP;
    }

    @Override
    public Expr getFirstChild() {
        return sequence;
    }

    public Expr getSequence() {
        return sequence;
    }

    public IdExpr getVar() {
        return var;
    }

    public Expr getTransformation() {
        return transformation;
    }

    @Override
    public String toString() {
        return super.toString(); // + toString(sequence, var, transformation);
    }
}
