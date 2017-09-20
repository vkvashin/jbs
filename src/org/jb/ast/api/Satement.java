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
public abstract class Satement extends JbNode {
    
    private Satement nextStatement;

    protected Satement(int line, int column) {
        super(line, column);
    }

    @Override
    public Satement getNextSibling() {
        return nextStatement;
    }

    public void setNextSibling(Satement nextSatement) {
        this.nextStatement = nextSatement;
    }
}
