package org.jb.lexer.api;

/**
 * Represents a sequence of tokens.
 * This could be just Iterator<JbToken>, but then we could not throw an exception
 * @author vkvashin
 */
public interface TokenStream {
    /** 
     * @return next token or Token.EOF if there are no tokens 
     */
    Token next() throws TokenStreamException;
}
