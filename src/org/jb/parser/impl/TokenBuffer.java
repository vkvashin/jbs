/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.impl;

import org.jb.lexer.api.Token;
import org.jb.lexer.api.TokenStreamException;
import org.jb.lexer.api.TokenStream;

/**
 *
 * @author vkvashin
 */
public abstract class TokenBuffer {

    protected final int maxLA;

    protected TokenBuffer(int maxLA) {
        this.maxLA = maxLA;
    }

    public final Token LA(int lookAhead) throws TokenStreamException {
        if (lookAhead > maxLA) {
            throw new TokenStreamException("Max lookahead exceeded: " + lookAhead + " while max=" + maxLA);
        }
        return LAImpl(lookAhead);
    }

    public abstract Token LAImpl(int lookAhead) throws TokenStreamException;

    public abstract void consume();
}
