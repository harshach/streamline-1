package com.hortonworks.streamline.streams.registry.table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RTATableMetaData {
    private static final Boolean DEFAULT_IS_FACT_TABLE = true;
    // TODO: revisit these default values
    private static final Integer DEFAULT_SLA = 2;
    private static final Integer DEFAULT_QPS = 10;
    @JsonProperty
    private Boolean isFactTable = DEFAULT_IS_FACT_TABLE;

    @JsonProperty
    private List<String> primaryKeys;

    @JsonProperty
    private Integer ingestionRate;

    @JsonProperty
    private Integer retentionDays;

    @JsonProperty
    private List<String> queryTypes;

    @JsonProperty
    private Integer slaSeconds = DEFAULT_SLA;

    @JsonProperty
    private String sourceName;

    @JsonProperty
    private Integer qps = DEFAULT_QPS;

    public Boolean isFactTable() {
        return isFactTable;
    }

    public void setFactTable(Boolean isFactTable) {
        this.isFactTable = isFactTable;
    }

    public List<String> primaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public Integer ingestionRate() {
        return ingestionRate;
    }

    public void setIngestionRate(Integer ingestionRate) {
        this.ingestionRate = ingestionRate;
    }

    public Integer retentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public List<String> queryTypes() {
        return queryTypes;
    }

    public void setQueryTypes(List<String> queryTypes) {
        this.queryTypes = queryTypes;
    }

    public Integer slaSeconds() {
        return slaSeconds;
    }

    public void setSlaSeconds(Integer slaSeconds) {
        this.slaSeconds = slaSeconds;
    }

    public String sourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Integer qps() {
        return qps;
    }

    public void setQps(Integer qps) {
        this.qps = qps;
    }
}
