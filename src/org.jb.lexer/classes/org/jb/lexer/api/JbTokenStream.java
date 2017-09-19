package org.jb.lexer.api;

/**
 * Represents a sequence of tokens.
 * This could be just Iterator<JbToken>, but then we could not throw an exception
 * @author vkvashin
 */
public interface JbTokenStream {
    /** @return next token or null if there are no tokens */
    JbToken next() throws JbTokenStreamException;
}
