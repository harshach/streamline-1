package com.hortonworks.streamline.streams.actions.athenax.topology.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RTATableField {
    @JsonProperty
    private String type;

    @JsonProperty
    private String name;

    @JsonProperty
    private String logicalType;

    @JsonProperty
    private String columnType;

    @JsonProperty
    private String cardinality;

    @JsonProperty
    private String doc;

    public String type() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String logicalType() {
        return logicalType;
    }

    public void setLogicalType(String logicalType) {
        this.logicalType = logicalType;
    }

    public String columnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String cardinality() {
        return cardinality;
    }

    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }

    public String doc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }
}
