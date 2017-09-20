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
public abstract class Literal extends Expr {

    private final CharSequence text;
    
    protected Literal(int line, int column, CharSequence text) {
        super(line, column);
        this.text = text;
    }    

    @Override
    public final JbNode getFirstChild() {
        return null;
    }

    public final CharSequence getText() {
        return text;
    }

    @Override
    public String toString() {
        return super.toString() + text;
    }
}
