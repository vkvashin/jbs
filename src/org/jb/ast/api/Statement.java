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
public abstract class Statement extends JbNode {
    
    private Statement nextStatement;

    protected Statement(int line, int column) {
        super(line, column);
    }

    @Override
    public Statement getNextSibling() {
        return nextStatement;
    }

    public void setNextSibling(Statement nextSatement) {
        this.nextStatement = nextSatement;
    }
}
