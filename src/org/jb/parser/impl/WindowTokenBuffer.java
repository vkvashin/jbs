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
public class WindowTokenBuffer extends TokenBuffer {

    private final Token[] tokens;
    private int pos;
    private int size;
    private TokenStream ts;

    public WindowTokenBuffer(TokenStream ts, int maxLA, int bufferSize) throws TokenStreamException {
        super(maxLA);
        this.ts = ts;
        assert bufferSize > maxLA;
        this.tokens = new Token[bufferSize];
        this.pos = 0;
        this.size = 0;
        fill();
    }

    private void shift() {
        for(int i = pos; i < size; i++) {
            tokens[i - pos] = tokens[i];
        }
        size -= pos;
        pos = 0;
    }

    private void fill() throws TokenStreamException {
        while (size < tokens.length) {
            Token tok = ts.next();
            if (Token.isEOF(tok)) {
                break;
            } else {
                tokens[size++] = tok;
            }
        }
    }

    @Override
    public void consume() {
        pos++;
    }

    @Override
    public Token LAImpl(int lookAhead) throws TokenStreamException {
        int idx = pos + lookAhead;
        if (idx > tokens.length - 1) {
            shift();
            fill();
            idx = pos + lookAhead;
        }
        Token tok = (idx < size) ? tokens[idx] : null;
        return tok;
    }
}
