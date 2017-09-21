/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.impl;

import org.jb.lexer.api.Token;
import org.jb.lexer.api.TokenStream;
import org.jb.lexer.api.TokenStreamException;

/**
 * It appeared that we do not need LA at all.
 * This class is created instead of changing parser interface to TokenStream instead of TokenBuffer
 * @author vkvashin
 */
public class ZeroLookaheadTokenBuffer extends TokenBuffer {

    private final TokenStream ts;
    private Token curr;
    
    public ZeroLookaheadTokenBuffer(TokenStream ts) {
        super(0);
        this.ts = ts;
        this.curr = null;
    }

    @Override
    public Token LAImpl(int lookAhead) throws TokenStreamException {
        return (curr == null) ? (curr = ts.next()) : curr;
    }

    @Override
    public void consume() {
        curr = null;
    }
}
