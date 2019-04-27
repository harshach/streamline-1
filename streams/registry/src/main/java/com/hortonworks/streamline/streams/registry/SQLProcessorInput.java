package com.hortonworks.streamline.streams.registry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hortonworks.registries.common.Schema;

import java.util.Map;

public class SQLProcessorInput {
    @JsonProperty
    private Map<String, Schema> inputSchemas;

    @JsonProperty
    private String sqlStatement;

    public Map<String, Schema> inputSchemas() {
        return inputSchemas;
    }

    public void setInputSchemas(Map<String, Schema> inputSchemas) {
        this.inputSchemas = inputSchemas;
    }

    public String sqlStatement() {
        return sqlStatement;
    }

    public void setSqlStatement(String sqlStatement) {
        this.sqlStatement = sqlStatement;
    }
}
