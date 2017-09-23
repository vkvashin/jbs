package org.jb.ast.api;

/**
 *
 * @author vkvashin
 */
public enum Type {
    INT("integer"),
    FLOAT("float"),
    SEQ_INT("integer sequence"),
    SEQ_FLOAT("float sequence"),
    STRING("sequence"),
    ERRONEOUS("erroneous");

    private final String displayName;

    private Type(String displayName) {
        this.displayName = displayName;
    }        

    public String getDisplayName() {
        return displayName;
    }    
}
