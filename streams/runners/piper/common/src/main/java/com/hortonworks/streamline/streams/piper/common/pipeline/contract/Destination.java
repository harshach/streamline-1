package com.hortonworks.streamline.streams.piper.common.pipeline.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Destination {

    @JsonProperty("type")
    private DestinationType type;
    @JsonProperty("connection_id")
    private String connectionId;

    // Only present for type == [hdfs, scp]
    @JsonProperty("format")
    private String format;
    @JsonProperty("path")
    private String path;

    // Only present for type == [hive, postgres]
    @JsonProperty("database_name")
    private String databaseName;
    @JsonProperty("table_name")
    private String tableName;

    // Only present for type == [hive, postgres], if mode == merge
    @JsonProperty("mode")
    private String mode;
    @JsonProperty("table_schema")
    private Object tableSchema;

    public DestinationType getType() {
        return type;
    }

    public void setType(DestinationType type) {
        this.type = type;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Object getTableSchema() {
        return tableSchema;
    }

    public void setTableSchema(Object tableSchema) {
        this.tableSchema = tableSchema;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Destination{");
        sb.append("type=").append(type);
        sb.append(", connectionId='").append(connectionId).append('\'');
        sb.append(", format='").append(format).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append(", databaseName='").append(databaseName).append('\'');
        sb.append(", tableName='").append(tableName).append('\'');
        sb.append(", mode='").append(mode).append('\'');
        sb.append(", tableSchema=").append(tableSchema);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Destination that = (Destination) o;
        return type == that.type &&
                Objects.equals(connectionId, that.connectionId) &&
                Objects.equals(format, that.format) &&
                Objects.equals(path, that.path) &&
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(tableName, that.tableName) &&
                Objects.equals(mode, that.mode) &&
                Objects.equals(tableSchema, that.tableSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, connectionId, format, path, databaseName, tableName, mode, tableSchema);
    }

    public enum DestinationType {

        HIVE("hive"),
        POSTGRES("postgres"),
        HDFS("hdfs"),
        SCP("scp");

        private final String value;
        private final static Map<String, Destination.DestinationType> CONSTANTS = new HashMap<String, Destination.DestinationType>();

        static {
            for (Destination.DestinationType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private DestinationType(String value) {
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
        public static Destination.DestinationType fromValue(String value) {
            Destination.DestinationType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }

}
