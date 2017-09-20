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
public class WindowTokenBuffer extends TokenBuffer {

    private final JbToken[] tokens;
    private int pos;
    private int size;
    private JbTokenStream ts;

    public WindowTokenBuffer(JbTokenStream ts, int maxLA, int bufferSize) throws JbTokenStreamException {
        super(maxLA);
        this.ts = ts;
        assert bufferSize > maxLA;
        this.tokens = new JbToken[bufferSize];
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

    private void fill() throws JbTokenStreamException {
        while (size < tokens.length) {
            JbToken tok = ts.next();
            if (tok == null) {
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
    public JbToken LAImpl(int lookAhead) throws JbTokenStreamException {
        int idx = pos + lookAhead;
        if (idx > tokens.length - 1) {
            shift();
            fill();
            idx = pos + lookAhead;
        }
        JbToken tok = (idx < size) ? tokens[idx] : null;
        return tok;
    }
}
