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
public final class PrintStatement extends Statement {

    private final StringLiteral string;

    public PrintStatement(int line, int column, StringLiteral string) {
        super(line, column);
        this.string = string;
    }

    @Override
    public NodeKind getNodeKind() {
        return NodeKind.PRINT;
    }

    @Override
    public StringLiteral getFirstChild() {
        return string;
    }

    public StringLiteral getString() {
        return string;
    }
}
