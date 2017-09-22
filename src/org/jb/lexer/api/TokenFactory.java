/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.lexer.api;

/**
 * A factory that creates tokens.
 * @author vkvashin
 */
public interface TokenFactory {
    public Token create(Token.Kind kind, CharSequence text, int line, int column);
    public Token createFixed(Token.Kind kind, int line, int column);
}
