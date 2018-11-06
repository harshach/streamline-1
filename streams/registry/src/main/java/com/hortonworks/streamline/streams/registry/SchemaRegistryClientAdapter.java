package com.hortonworks.streamline.streams.registry;

import com.hortonworks.registries.schemaregistry.SchemaBranch;
import com.hortonworks.registries.schemaregistry.SchemaIdVersion;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.SchemaVersion;
import com.hortonworks.registries.schemaregistry.SchemaVersionInfo;
import com.hortonworks.registries.schemaregistry.SchemaVersionKey;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.errors.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.errors.SchemaBranchNotFoundException;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SchemaRegistryClientAdapter implements StreamlineSchemaRegistryClient {
    private SchemaRegistryClient schemaRegistryClient;

    public SchemaRegistryClientAdapter(SchemaRegistryClient schemaRegistryClient) {
        this.schemaRegistryClient = schemaRegistryClient;
    }

    @Override
    public List<String> getSchemaBranchNames(String schemaName) throws StreamlineSchemaNotFoundException {
        Collection<SchemaBranch> schemaBranches;
        try {
            schemaBranches = schemaRegistryClient.getSchemaBranches(schemaName);
            if (schemaBranches == null || schemaBranches.isEmpty()) {
                throw new SchemaNotFoundException(String.format("Cannot find schema with name: %s", schemaName));
            }
        } catch (SchemaNotFoundException e) {
            throw new StreamlineSchemaNotFoundException(e);
        }
        return schemaBranches.stream().map(SchemaBranch::getName).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getSchemaVersions(String schemaName, String schemaBranchName) throws StreamlineSchemaNotFoundException {
        Collection<SchemaVersionInfo> schemaVersionInfos;
        try {
            schemaVersionInfos = schemaRegistryClient.getAllVersions(effectiveBranchName(schemaBranchName), schemaName);
            if (schemaVersionInfos == null || schemaVersionInfos.isEmpty()) {
                throw new SchemaNotFoundException(String.format("Cannot find schema with name: %s and branch: %s", schemaName, schemaBranchName));
            }
        } catch (SchemaNotFoundException e) {
            throw new StreamlineSchemaNotFoundException(e);
        }
        return schemaVersionInfos.stream().map(SchemaVersionInfo::getVersion).collect(Collectors.toList());
    }

    @Override
    public String getSchema(String schemaName, Integer schemaVersion) throws StreamlineSchemaNotFoundException {
        SchemaVersionInfo schemaVersionInfo;
        try {
            schemaVersionInfo = schemaRegistryClient.getSchemaVersionInfo(new SchemaVersionKey(schemaName, schemaVersion));
            if (schemaVersionInfo == null) {
                throw new SchemaNotFoundException(String.format("Cannot find schema with name: %s and version: %d", schemaName, schemaVersion));
            }
        } catch (SchemaNotFoundException e) {
            throw new StreamlineSchemaNotFoundException(e);
        }
        return schemaVersionInfo.getSchemaText();
    }

    private String effectiveBranchName(String branchName) {
        return branchName == null || branchName.isEmpty() ? SchemaBranch.MASTER_BRANCH : branchName;
    }

    public Long registerSchemaMetadata(SchemaMetadata schemaMetadata) {
        return schemaRegistryClient.registerSchemaMetadata(schemaMetadata);
    }

    public SchemaIdVersion addSchemaVersion(final String schemaName, final SchemaVersion schemaVersion)
            throws InvalidSchemaException, IncompatibleSchemaException, SchemaNotFoundException, SchemaBranchNotFoundException {
        return schemaRegistryClient.addSchemaVersion(schemaName, schemaVersion);
    }
}

