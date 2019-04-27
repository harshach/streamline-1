package com.hortonworks.streamline.streams.registry;

import org.apache.flink.table.annotation.TableType;
import org.apache.flink.table.catalog.ExternalCatalogTable;
import org.apache.flink.table.catalog.TableSourceConverter;

import java.util.Collections;
import java.util.Set;

@TableType(Constants.TABLE_TYPE)
public class SchemaTableSourceConverter implements TableSourceConverter<SchemaTableSource> {
    @Override
    public Set<String> requiredProperties() {
        return Collections.emptySet();
    }

    @Override
    public SchemaTableSource fromExternalCatalogTable(ExternalCatalogTable externalCatalogTable) {
        return new SchemaTableSource(externalCatalogTable.schema());
    }
}
