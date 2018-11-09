package com.hortonworks.streamline.streams.registry;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface StreamlineSchemaRegistryClient {
    void init(Map<String, Object> config);

    List<String> getSchemaBranchNames(String schemaName) throws StreamlineSchemaNotFoundException, IOException;

    List<Integer> getSchemaVersions(String schemaName, String schemaBranchName) throws StreamlineSchemaNotFoundException, IOException;

    String getSchema(String schemaName, Integer schemaVersion) throws StreamlineSchemaNotFoundException, IOException;
}
