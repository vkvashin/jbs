/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.impl;

import org.jb.lexer.api.JbToken;
import org.jb.lexer.api.JbTokenStream;
import org.jb.lexer.api.JbTokenStreamException;

/**
 *
 * @author vkvashin
 */
public abstract class TokenBuffer {

    protected final int maxLA;

    protected TokenBuffer(int maxLA) {
        this.maxLA = maxLA;
    }

    public final JbToken LA(int lookAhead) throws JbTokenStreamException {
        if (lookAhead > maxLA) {
            throw new JbTokenStreamException("Max lookahead exceeded: " + lookAhead + " while max=" + maxLA);
        }
        return LAImpl(lookAhead);
    }

    public abstract JbToken LAImpl(int lookAhead) throws JbTokenStreamException;

    public abstract void consume();
}
