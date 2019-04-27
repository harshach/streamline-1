package com.hortonworks.streamline.streams.registry;

import org.apache.flink.table.api.CatalogNotExistException;
import org.apache.flink.table.api.TableNotExistException;
import org.apache.flink.table.catalog.ExternalCatalog;
import org.apache.flink.table.catalog.ExternalCatalogTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SchemaCatalog implements ExternalCatalog {
    private final Map<String, ExternalCatalogTable> tables;

    public SchemaCatalog(Map<String, ExternalCatalogTable> tables) {
        this.tables = tables;
    }

    @Override
    public ExternalCatalogTable getTable(String tableName) throws TableNotExistException {
        return tables.get(tableName);
    }

    @Override
    public List<String> listTables() {
        return new ArrayList<>(tables.keySet());
    }

    @Override
    public ExternalCatalog getSubCatalog(String dbName) throws CatalogNotExistException {
        throw new CatalogNotExistException(dbName);
    }

    @Override
    public List<String> listSubCatalogs() {
        return Collections.emptyList();
    }
}
