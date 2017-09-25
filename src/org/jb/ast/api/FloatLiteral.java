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
public final class FloatLiteral extends Literal {

    private final double value;

    public FloatLiteral(int line, int column, CharSequence text, double value) {
        super(line, column, text);
        this.value = value;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.FLOAT;
    }    

    @Override
    public Type getType() {
        return Type.FLOAT;
    }

    public double getValue() {
        return value;
    }
}
