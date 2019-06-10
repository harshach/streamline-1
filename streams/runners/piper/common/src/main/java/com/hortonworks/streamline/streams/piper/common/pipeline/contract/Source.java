package com.hortonworks.streamline.streams.piper.common.pipeline.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hortonworks.streamline.streams.piper.common.pipeline.Pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Source {

    @JsonProperty("type")
    private SourceType type;
    @JsonProperty("connection_id")
    private String connectionId;
    @JsonProperty("sql")
    private String sql;
    @JsonProperty("querybuilder_uuid")
    private String querybuilderUuid;
    @JsonProperty("querybuilder_report_id")
    private String querybuilderReportId;

    public SourceType getType() {
        return type;
    }

    public void setType(SourceType type) {
        this.type = type;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getQuerybuilderUuid() {
        return querybuilderUuid;
    }

    public void setQuerybuilderUuid(String querybuilderUuid) {
        this.querybuilderUuid = querybuilderUuid;
    }

    public String getQuerybuilderReportId() { return querybuilderReportId; }

    public void setQuerybuilderReportId(String querybuilderReportId) { this.querybuilderReportId = querybuilderReportId; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Source{");
        sb.append("type=").append(type);
        sb.append(", connectionId='").append(connectionId).append('\'');
        sb.append(", sql='").append(sql).append('\'');
        sb.append(", querybuilderUuid='").append(querybuilderUuid).append('\'');
        sb.append(", queryBuilderReportId='").append(querybuilderReportId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Source source = (Source) o;
        return type == source.type &&
                Objects.equals(connectionId, source.connectionId) &&
                Objects.equals(sql, source.sql) &&
                Objects.equals(querybuilderUuid, source.querybuilderUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, connectionId, sql, querybuilderUuid, querybuilderReportId);
    }

    public enum SourceType {

        VERTICA("vertica"),
        HIVE("hive"),
        POSTGRES("postgres"),
        HIVESCRIPT("hive_script");

        private final String value;
        private final static Map<String, Source.SourceType> CONSTANTS = new HashMap<String, Source.SourceType>();

        static {
            for (Source.SourceType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private SourceType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Source.SourceType fromValue(String value) {
            Source.SourceType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }

}
