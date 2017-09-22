/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.impl;

import java.util.ArrayList;
import org.jb.lexer.api.Token;
import org.jb.lexer.api.TokenStreamException;
import org.jb.lexer.api.TokenStream;

/**
 *
 * @author vkvashin
 */
public class ArrayTokenBuffer extends TokenBuffer {

    private final ArrayList<Token> tokens;
    private int pos;

    public ArrayTokenBuffer(TokenStream ts, int maxLA) throws TokenStreamException {
        super(maxLA);
        tokens = new ArrayList<>();
        pos = 0;
        fill(ts);
    }

    private void fill(TokenStream ts) throws TokenStreamException {
        for (Token tok = ts.next(); !Token.isEOF(tok); tok = ts.next()) {
            tokens.add(tok);
        }
    }

    @Override
    public void consume() {
        pos++;
    }

    @Override
    public Token LAImpl(int lookAhead) throws TokenStreamException {
        int idx = pos + lookAhead;
        Token tok = (idx < tokens.size()) ? tokens.get(idx) : null;
        return tok;
    }
}
