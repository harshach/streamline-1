package com.hortonworks.streamline.storage;

/**
 * Represents component's parent information .
 */
public class Parent {
    public Parent(Long parentID, String parentType) {
        this.id = parentID;
        this.type = parentType;
    }
    /**
     * Parent ID for this component.
     */
    private Long id;

    /**
     * Type to identify parent object type.
     */
    private String type;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
