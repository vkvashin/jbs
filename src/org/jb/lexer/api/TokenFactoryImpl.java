/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.lexer.api;

/**
 * Package-lecel class: let only lexer create tokens.
 * The instance of this class will be handed to clients who are allowed to create tokens 
 * (only Lexer so far)
 * @author vkvashin
 */
/*package*/ class TokenFactoryImpl implements TokenFactory {

    /*package*/ TokenFactoryImpl() {
    }

    /**
     * In order to make optimizations without changing clients code possible,
     * use factory method instead of ctor
     */
    @Override
    public Token create(Token.Kind kind, CharSequence text, int line, int column) {
        // until we use smart charsequences text is always string.
        // by text.toString() below we guarantee classes like StringBuilder 
        // (without goot equals and hash) don't get inside
        return new Token(kind, text.toString(), line, column);
    }

    @Override
    public Token createFixed(Token.Kind kind, int line, int column) {
        assert kind.isFixedText();
        return new Token(kind, kind.getFixedText(), line, column);
    }
}
