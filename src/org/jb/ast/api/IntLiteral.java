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
public final class IntLiteral extends Literal {

    private final int value;

    public IntLiteral(int line, int column, CharSequence text, int value) {
        super(line, column, text);
        this.value = value;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.INT;
    }    
    
    @Override
    public Type getType() {
        return Type.INT;
    }

    public int getValue() {
        return value;
    }    
}
