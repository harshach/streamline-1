package com.hortonworks.streamline.streams.registry.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RTATableField {
    @JsonProperty
    private String type;

    @JsonProperty
    private String name;

    @JsonProperty
    private String uberLogicalType;

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

    public String uberLogicalType() {
        return uberLogicalType;
    }

    public void setUberLogicalType(String uberLogicalType) {
        this.uberLogicalType = uberLogicalType;
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
