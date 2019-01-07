package com.hortonworks.streamline.streams.actions.athenax.topology.entity;

public enum RTAQueryTypes {
    PRE_DEFINED("pre-defined", "supportPredefinedQueries"),
    ADHOC("adhoc", "supportAdhocQueries");

    private final String rtaQueryTypeName;
    private final String uiFieldName;

    RTAQueryTypes(String rtaQueryTypeName, String uiFieldName) {
        this.rtaQueryTypeName = rtaQueryTypeName;
        this.uiFieldName = uiFieldName;
    }

    public String getRtaQueryTypeName() {
        return rtaQueryTypeName;
    }

    public String getUiFieldName() {
        return uiFieldName;
    }
}
