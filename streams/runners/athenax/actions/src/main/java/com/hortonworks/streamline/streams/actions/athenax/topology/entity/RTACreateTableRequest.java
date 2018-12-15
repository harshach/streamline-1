package com.hortonworks.streamline.streams.actions.athenax.topology.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RTACreateTableRequest {
    private static final String DEFAULT_NAMESPACE = "RTA";
    private static final String DEFAULT_TYPE = "record";

    @JsonProperty
    private String owner;

    @JsonProperty
    private String namespace = DEFAULT_NAMESPACE;

    @JsonProperty
    private String name;

    @JsonProperty
    private RTATableMetaData rtaTableMetaData;

    @JsonProperty
    private List<RTATableField> fields;

    @JsonProperty
    private String type = DEFAULT_TYPE;

    public String owner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String namespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RTATableMetaData rtaTableMetaData() {
        return rtaTableMetaData;
    }

    public void setRtaTableMetaData(RTATableMetaData rtaTableMetaData) {
        this.rtaTableMetaData = rtaTableMetaData;
    }

    public List<RTATableField> fields() {
        return fields;
    }

    public void setFields(List<RTATableField> fields) {
        this.fields = fields;
    }

    public String type() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
