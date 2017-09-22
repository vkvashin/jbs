package org.jb.lexer.api;

/**
 * An exception that occurs when lexing.
 * It means that an error that prevents further lexing has occurred
 * @author vkvashin
 */
public final class TokenStreamException extends Exception {

    public TokenStreamException(String message) {
        super(message);
    }

    public TokenStreamException(Throwable cause) {
        super(cause);
    }   
}
