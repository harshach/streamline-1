package com.hortonworks.streamline.streams.registry;

import com.uber.data.heatpipe.configuration.Heatpipe4JConfig;
import com.uber.data.heatpipe.configuration.YamlHeatpipeConfiguration;
import com.uber.data.heatpipe.errors.SchemaServiceNotAvailableException;
import com.uber.data.heatpipe.schema.SchemaNotFoundException;
import com.uber.data.heatpipe.schema.SchemaServiceClient;
import com.uber.data.heatpipe.schema.SchemaServiceFactory;
import com.uber.data.heatpipe.util.SchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.EMPTY_LIST;

public class HeatpipeSchemaServiceClientAdapter implements StreamlineSchemaRegistryClient {
    private static final Logger LOG = LoggerFactory.getLogger(HeatpipeSchemaServiceClientAdapter.class);

    private SchemaServiceClient schemaServiceClient;

    public void init(Map<String, Object> config) {
        Heatpipe4JConfig heatpipe4JConfig = new YamlHeatpipeConfiguration.Builder().setAppId("uworc").build();
        SchemaServiceClient schemaServiceClient = null;
        try {
            schemaServiceClient = SchemaServiceFactory.getClient(heatpipe4JConfig);
        } catch (SchemaServiceNotAvailableException e) {
            LOG.error("Schema Service (heatpipe) is not available!", e);
        }
        this.schemaServiceClient = schemaServiceClient;
    }

    @Override
    public List<String> getSchemaBranchNames(String schemaName) {
        return EMPTY_LIST;
    }

    @Override
    public List<Integer> getSchemaVersions(String schemaName, String schemaBranchName) throws StreamlineSchemaNotFoundException, IOException {
        if (schemaServiceClient == null) {
            throw new StreamlineSchemaServiceNotAvailableException("Failed to get schema versions. Schema Service client was not initialized correctly.");
        }
        try {
            return schemaServiceClient.getSchemaVersions(schemaName);
        } catch (SchemaNotFoundException e) {
            throw new StreamlineSchemaNotFoundException(e);
        }
    }

    @Override
    public String getSchema(String schemaName, Integer schemaVersion) throws StreamlineSchemaNotFoundException, IOException {
        if (schemaServiceClient == null) {
            throw new StreamlineSchemaServiceNotAvailableException("Failed to get schema. Schema Service client was not initialized correctly.");
        }
        try {
            return schemaServiceClient.getSchemaText(schemaName, schemaVersion, SchemaType.CONSUMER);
        } catch (SchemaNotFoundException e) {
            throw new StreamlineSchemaNotFoundException(e);
        }
    }
}

