package com.hortonworks.streamline.streams.registry;

import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.typeinfo.BasicArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.MapTypeInfo;
import org.apache.flink.api.java.typeutils.ObjectArrayTypeInfo;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.catalog.ExternalCatalog;
import org.apache.flink.table.catalog.ExternalCatalogTable;
import com.hortonworks.registries.common.Schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.hortonworks.registries.common.Schema.Type.BOOLEAN;
import static com.hortonworks.registries.common.Schema.Type.BYTE;
import static com.hortonworks.registries.common.Schema.Type.DOUBLE;
import static com.hortonworks.registries.common.Schema.Type.FLOAT;
import static com.hortonworks.registries.common.Schema.Type.INTEGER;
import static com.hortonworks.registries.common.Schema.Type.LONG;
import static com.hortonworks.registries.common.Schema.Type.SHORT;
import static com.hortonworks.registries.common.Schema.Type.STRING;


public class SchemaServiceUtil {

    private static final EnumMap<Schema.Type, TypeInformation> ATOMIC_TYPES = new EnumMap<>(
            ImmutableMap.<Schema.Type, TypeInformation>builder()
                    .put(BOOLEAN, BasicTypeInfo.BOOLEAN_TYPE_INFO)
                    .put(BYTE, BasicTypeInfo.BYTE_TYPE_INFO)
                    .put(SHORT, BasicTypeInfo.SHORT_TYPE_INFO)
                    .put(INTEGER, BasicTypeInfo.INT_TYPE_INFO)
                    .put(LONG, BasicTypeInfo.LONG_TYPE_INFO)
                    .put(FLOAT, BasicTypeInfo.FLOAT_TYPE_INFO)
                    .put(DOUBLE, BasicTypeInfo.DOUBLE_TYPE_INFO)
                    .put(STRING, BasicTypeInfo.STRING_TYPE_INFO)
                    .build()
    );

    private static final ImmutableMap<BasicTypeInfo, Schema.Type> STREAMLINE_ATOMIC_TYPES;

    static {
        ImmutableMap.Builder<BasicTypeInfo, Schema.Type> builder = ImmutableMap.builder();
        ATOMIC_TYPES.forEach((streamlineType, flinkType) -> builder.put((BasicTypeInfo) flinkType, streamlineType));
        STREAMLINE_ATOMIC_TYPES = builder.build();
    }

    private static String getTableName(String topicName) {
        return topicName.replaceAll("[-.]", "_");
    }

    private static TypeInformation<?> fromStreamlineSchemaField(Schema.Field field) {
        final TypeInformation<?> typeInfo;
        switch (field.getType()) {
            case STRING:
            case DOUBLE:
            case LONG:
            case BOOLEAN:
                typeInfo = ATOMIC_TYPES.get(field.getType());
                break;

            case BINARY:  // BYTES
                typeInfo = BasicArrayTypeInfo.BYTE_ARRAY_TYPE_INFO;
                break;

            case NESTED:  // MAP
                assert field instanceof Schema.NestedField;
                Schema.NestedField nestedField = (Schema.NestedField) field;
                typeInfo = new MapTypeInfo<>(BasicTypeInfo.STRING_TYPE_INFO, fromStreamlineSchemaFields(nestedField.getFields()));
                break;

            case ARRAY:
                assert field instanceof Schema.ArrayField;
                Schema.ArrayField arrayField = (Schema.ArrayField) field;
                typeInfo = ObjectArrayTypeInfo.getInfoFor(fromStreamlineSchemaFields(arrayField.getMembers()));
                break;

            default:
                typeInfo = null;
        }
        return typeInfo;
    }

    private static TypeInformation<?> fromStreamlineSchemaFields(List<Schema.Field> fields) {
        List<TypeInformation<?>> types = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Schema.Field field : fields) {
            TypeInformation<?> typeInformation = fromStreamlineSchemaField(field);
            if (typeInformation != null) {
                types.add(typeInformation);
                names.add(field.getName());
            }
        }
        return new RowTypeInfo(types.toArray(new TypeInformation<?>[types.size()]), names.toArray(new String[names.size()]));
    }

    private static RowTypeInfo wrapAsHpRowType(RowTypeInfo datumRowType) {
        return new RowTypeInfo(new TypeInformation[]{
                BasicTypeInfo.DOUBLE_TYPE_INFO,
                BasicTypeInfo.STRING_TYPE_INFO,
                BasicTypeInfo.STRING_TYPE_INFO,
                BasicTypeInfo.STRING_TYPE_INFO,
                datumRowType,
                SqlTimeTypeInfo.TIMESTAMP,
                SqlTimeTypeInfo.TIMESTAMP
        }, new String[] {"ts", "host", "dc", "uuid", "msg", "rowtime", "proctime"});
    }

    private static Schema toStreamlineSchema(TableSchema outputSchema) {
        return toStreamlineSchema(new RowTypeInfo(outputSchema.getTypes(), outputSchema.getColumnNames()));
    }

    private static Schema toStreamlineSchema(TypeInformation<?> type) {
        RowTypeInfo rowType = (RowTypeInfo) type;
        String[] fieldNames = rowType.getFieldNames();
        List<Schema.Field> fields = IntStream.range(0, rowType.getArity()).mapToObj(i ->
                new Schema.Field(fieldNames[i], STREAMLINE_ATOMIC_TYPES.get(rowType.getTypeAt(i)))
        ).collect(Collectors.toList());
        return new Schema.SchemaBuilder().fields(fields).build();
    }

    public static Schema getOutputSchema(Map<String, Schema> inputSchemas, String sqlStatement) {
        StreamExecutionEnvironment execEnv = LocalStreamEnvironment.getExecutionEnvironment();
        TableEnvironment env = TableEnvironment.getTableEnvironment(execEnv);

        Map<String, ExternalCatalogTable> tableMap = new HashMap<>();
        for (Map.Entry<String, Schema> entry : inputSchemas.entrySet()) {
            String inputSchemaName = entry.getKey();
            Schema inputSchema = entry.getValue();
            String tableName = getTableName(inputSchemaName);

            RowTypeInfo datumRowType = (RowTypeInfo) fromStreamlineSchemaFields(inputSchema.getFields());
            RowTypeInfo hpRowType = wrapAsHpRowType(datumRowType);
            TableSchema tableSchema = new TableSchema(hpRowType.getFieldNames(), hpRowType.getFieldTypes());

            ExternalCatalogTable catalogTable = new ExternalCatalogTable(Constants.TABLE_TYPE, tableSchema, Collections.emptyMap(), null, null, null, null);
            tableMap.put(tableName, catalogTable);
        }
        ExternalCatalog catalog = new SchemaCatalog(tableMap);
        env.registerExternalCatalog(Constants.CATALOG_NAME, catalog);

        Table outputTable = env.sqlQuery(sqlStatement);
        TableSchema outputSchema = outputTable.getSchema();
        return toStreamlineSchema(outputSchema);
    }
}
