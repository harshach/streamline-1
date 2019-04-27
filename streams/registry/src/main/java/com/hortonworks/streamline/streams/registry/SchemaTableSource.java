package com.hortonworks.streamline.streams.registry;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.sources.StreamTableSource;
import org.apache.flink.types.Row;

public class SchemaTableSource implements StreamTableSource<Row> {

    private TableSchema tableSchema;
    private TypeInformation<Row> returnType;

    public SchemaTableSource(RowTypeInfo rowTypeInfo) {
        tableSchema = new TableSchema(rowTypeInfo.getFieldNames(), rowTypeInfo.getFieldTypes());
        returnType = rowTypeInfo;
    }

    public SchemaTableSource(TableSchema tableSchema) {
        this.tableSchema = tableSchema;
        this.returnType = new RowTypeInfo(tableSchema.getTypes(), tableSchema.getColumnNames());
    }

    @Override
    public DataStream<Row> getDataStream(StreamExecutionEnvironment execEnv) {
        return null;
    }

    @Override
    public TypeInformation<Row> getReturnType() {
        return returnType;
    }

    @Override
    public TableSchema getTableSchema() {
        return tableSchema;
    }

    @Override
    public String explainSource() {
        return "Source with schema only";
    }
}
