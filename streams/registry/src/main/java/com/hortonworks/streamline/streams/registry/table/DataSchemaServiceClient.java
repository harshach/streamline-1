package com.hortonworks.streamline.streams.registry.table;

public interface DataSchemaServiceClient {
    String getTableSchema(String tableName) throws TableNotFoundException;

    void createTable(Object createTableRequest) throws CreateTableException;

    void deployTable(Object deployTableRequest, String tableName) throws DeployTableException;
}
