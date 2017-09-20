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

    public FloatLiteral(int line, int column, CharSequence text) {
        super(line, column, text);
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.FLOAT;
    }    
}
