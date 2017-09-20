/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.impl;

import java.util.ArrayList;
import org.jb.lexer.api.JbToken;
import org.jb.lexer.api.JbTokenStream;
import org.jb.lexer.api.JbTokenStreamException;

/**
 *
 * @author vkvashin
 */
public class ArrayTokenBuffer extends TokenBuffer {

    private final ArrayList<JbToken> tokens;
    private int pos;

    public ArrayTokenBuffer(JbTokenStream ts, int maxLA) throws JbTokenStreamException {
        super(maxLA);
        tokens = new ArrayList<>();
        pos = 0;
        fill(ts);
    }

    private void fill(JbTokenStream ts) throws JbTokenStreamException {
        for (JbToken tok = ts.next(); tok != null; tok = ts.next()) {
            tokens.add(tok);
        }
    }

    @Override
    public void consume() {
        pos++;
    }

    @Override
    public JbToken LAImpl(int lookAhead) throws JbTokenStreamException {
        int idx = pos + lookAhead;
        JbToken tok = (idx < tokens.size()) ? tokens.get(idx) : null;
        return tok;
    }
}
