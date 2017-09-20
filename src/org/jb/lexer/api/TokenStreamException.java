package org.jb.lexer.api;

/**
 * An exception that occurs when lexing
 * @author vkvashin
 */
public final class TokenStreamException extends Exception {

    public TokenStreamException() {
    }

    public TokenStreamException(String message) {
        super(message);
    }

    public TokenStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenStreamException(Throwable cause) {
        super(cause);
    }   
}
