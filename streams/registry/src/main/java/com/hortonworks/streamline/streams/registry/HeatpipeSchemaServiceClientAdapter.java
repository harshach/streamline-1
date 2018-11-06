package com.hortonworks.streamline.streams.registry;

import com.uber.data.heatpipe.schema.SchemaNotFoundException;
import com.uber.data.heatpipe.schema.SchemaServiceClient;
import com.uber.data.heatpipe.util.SchemaType;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;

public class HeatpipeSchemaServiceClientAdapter implements StreamlineSchemaRegistryClient {
    private SchemaServiceClient schemaServiceClient;

    public HeatpipeSchemaServiceClientAdapter(SchemaServiceClient schemaServiceClient) {
        this.schemaServiceClient = schemaServiceClient;
    }

    @Override
    public List<String> getSchemaBranchNames(String schemaName) {
        return EMPTY_LIST;
    }

    @Override
    public List<Integer> getSchemaVersions(String schemaName, String schemaBranchName) throws StreamlineSchemaNotFoundException, IOException {
        try {
            return schemaServiceClient.getSchemaVersions(schemaName);
        } catch (SchemaNotFoundException e) {
            throw new StreamlineSchemaNotFoundException(e);
        }
    }

    @Override
    public String getSchema(String schemaName, Integer schemaVersion) throws StreamlineSchemaNotFoundException, IOException {
        try {
            return schemaServiceClient.getSchemaText(schemaName, schemaVersion, SchemaType.CONSUMER);
        } catch (SchemaNotFoundException e) {
            throw new StreamlineSchemaNotFoundException(e);
        }
    }
}

