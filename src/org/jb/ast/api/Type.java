package org.jb.ast.api;

/**
 *
 * @author vkvashin
 */
public enum Type {
    INT("integer"),
    FLOAT("float"),
    SEQUENCE("sequence"),
    STRING("sequence"),
    /** It's a temporary workaround and should be removed */
    UNKNOWN("unknown"),
    ERRONEOUS("erroneous");

    private final String displayName;

    private Type(String displayName) {
        this.displayName = displayName;
    }        

    public String getDisplayName() {
        return displayName;
    }    
}
