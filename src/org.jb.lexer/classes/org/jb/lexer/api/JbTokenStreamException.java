package org.jb.lexer.api;

/**
 * An exception that occurs when lexing
 * @author vkvashin
 */
public final class JbTokenStreamException extends Exception {

    public JbTokenStreamException() {
    }

    public JbTokenStreamException(String message) {
        super(message);
    }

    public JbTokenStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public JbTokenStreamException(Throwable cause) {
        super(cause);
    }   
}
