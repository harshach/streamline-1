package com.hortonworks.streamline.streams.layout.component;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ValidationStatus {
    /**
     * componentId in which this error occurred.
     */
    @JsonProperty
    private String componentName;

    /**
     * getContainError to indicate if the component has error or not.
     */
    @JsonProperty
    private boolean containError;

    /**
     * listErrorMsg to hold the list of error messages.
     */
    @JsonProperty
    private Map<String, String> listErrorMsg;

    public ValidationStatus () {
        listErrorMsg = new HashMap<>();
        containError = false;
    }

    public void addErrorMessage(String fieldId, String errorMessage) {
        listErrorMsg.putIfAbsent(fieldId, errorMessage);
    }

    public void setContainError(boolean containError) {
        this.containError = containError;
    }

    public boolean getContainError() {
        return containError;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
}
