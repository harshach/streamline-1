package com.hortonworks.streamline.streams.actions.athenax.topology.entity;

public enum RTAQueryTypes {
    PRE_DEFINED("pre-defined", "supportPredefinedQueries"),
    ADHOC("adhoc", "supportAdhocQueries");

    private final String rtaTypeName;
    private final String uiFieldName;

    RTAQueryTypes(String rtaTypeName, String uiFieldName) {
        this.rtaTypeName = rtaTypeName;
        this.uiFieldName = uiFieldName;
    }

    public String getRtaTypeName() {
        return rtaTypeName;
    }

    public String getUiFieldName() {
        return uiFieldName;
    }
}
