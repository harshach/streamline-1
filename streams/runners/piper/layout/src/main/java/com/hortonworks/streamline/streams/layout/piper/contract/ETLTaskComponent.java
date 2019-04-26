package com.hortonworks.streamline.streams.layout.piper.contract;

import com.hortonworks.streamline.streams.piper.common.pipeline.contract.Destination;
import com.hortonworks.streamline.streams.piper.common.pipeline.contract.Source;

import java.util.HashMap;
import java.util.Map;

public class ETLTaskComponent extends AbstractPiperContractTaskComponent {

    private static final String SOURCE_TYPE_HIVE_OPTION = "sourceTypeHiveOption";
    private static final String HIVE_SOURCE_CONNECTION_ID = "hive.source.connection_id";
    private static final String SOURCE_TYPE_VERTICA_OPTION = "sourceTypeVerticaOption";
    private static final String VERTICA_SOURCE_CONNECTION_ID = "vertica.source.connection_id";
    private static final String SOURCE_TYPE_POSTGRES_OPTION = "sourceTypePostgresOption";
    private static final String POSTGRES_SOURCE_CONNECTION_ID = "postgres.source.connection_id";
    private static final String SOURCE_TYPE = "sourceType";
    private static final String DESTINATION_TYPE = "destinationType";
    private static final String DESTINATION_TYPE_HIVE_OPTION = "destinationTypeHiveOption";
    private static final String HIVE_DESTINATION_CONNECTION_ID = "hive.destination.connection_id";
    private static final String HIVE_DESTINATION_DATABASE_NAME = "hive.destination.database_name";
    private static final String HIVE_DESTINATION_TABLE_NAME = "hive.destination.table_name";
    private static final String HIVE_DESTINATION = "hive.destination";
    private static final String DESTINATION_TYPE_POSTGRES_OPTION = "destinationTypePostgresOption";
    private static final String POSTGRES_DESTINATION_CONNECTION_ID = "postgres.destination.connection_id";
    private static final String POSTGRES_DESTINATION_DATABASE_NAME = "postgres.destination.database_name";
    private static final String POSTGRES_DESTINATION_TABLE_NAME = "postgres.destination.table_name";
    private static final String POSTGRES_DESTINATION = "postgres.destination";
    private static final String DESTINATION_TYPE_HDFS_OPTION = "destinationTypeHdfsOption";
    private static final String HDFS_DESTINATION_CONNECTION_ID = "hdfs.destination.connection_id";
    private static final String HDFS_DESTINATION_PATH = "hdfs.destination.path";
    private static final String CSV = "csv";
    private static final String DESTINATION_TYPE_SCP_OPTION = "destinationTypeScpOption";
    private static final String SCP_DESTINATION_CONNECTION_ID = "scp.destination.connection_id";
    private static final String SCP_DESTINATION_PATH = "scp.destination.path";
    private static final String MERGE = "merge";

    @Override
    public Source generateSource() {
        Source source = new Source();

        Map<String, Object> sourceTypeProp = (Map<String, Object>) config.getOrDefault(SOURCE_TYPE, new HashMap());
        Map.Entry<String, Object> entry = sourceTypeProp.entrySet().iterator().next();
        String sourceType = entry.getKey();
        Map<String, Object> sourceParams = (Map<String, Object>) entry.getValue();

        String connectionKey = "";
        if (sourceType.equals(SOURCE_TYPE_HIVE_OPTION)) {
            source.setType(Source.SourceType.HIVE);
            connectionKey = HIVE_SOURCE_CONNECTION_ID;
        } else if (sourceType.equals(SOURCE_TYPE_VERTICA_OPTION)) {
            source.setType(Source.SourceType.VERTICA);
            connectionKey = VERTICA_SOURCE_CONNECTION_ID;
        } else if (sourceType.equals(SOURCE_TYPE_POSTGRES_OPTION)) {
            source.setType(Source.SourceType.POSTGRES);
            connectionKey = POSTGRES_SOURCE_CONNECTION_ID;
        }
        source.setConnectionId((String)sourceParams.get(connectionKey));

        setScriptType(source);

        return source;
    }

    @Override
    public Destination generateDestination() {
        Destination destination = new Destination();

        Map<String, Object> destinationTypeProp = (Map<String, Object>)
                config.getOrDefault(DESTINATION_TYPE, new HashMap());
        Map.Entry<String, Object> entry = destinationTypeProp.entrySet().iterator().next();
        String destinationType = entry.getKey();
        Map<String, Object> destinationParams = (Map<String, Object>) entry.getValue();

        if (destinationType.equals(DESTINATION_TYPE_HIVE_OPTION)) {
            destination.setType(Destination.DestinationType.HIVE);
            destination.setConnectionId((String)destinationParams.get(HIVE_DESTINATION_CONNECTION_ID));
            destination.setDatabaseName((String)destinationParams.get(HIVE_DESTINATION_DATABASE_NAME));
            destination.setTableName((String)destinationParams.get(HIVE_DESTINATION_TABLE_NAME));
            setMode(destination, destinationParams, HIVE_DESTINATION);
        } else if (destinationType.equals(DESTINATION_TYPE_POSTGRES_OPTION)) {
            destination.setType(Destination.DestinationType.POSTGRES);
            destination.setConnectionId((String)destinationParams.get(POSTGRES_DESTINATION_CONNECTION_ID));
            destination.setDatabaseName((String)destinationParams.get(POSTGRES_DESTINATION_DATABASE_NAME));
            destination.setTableName((String)destinationParams.get(POSTGRES_DESTINATION_TABLE_NAME));
            setMode(destination, destinationParams, POSTGRES_DESTINATION);
        } else if (destinationType.equals(DESTINATION_TYPE_HDFS_OPTION)) {
            destination.setConnectionId((String)destinationParams.get(HDFS_DESTINATION_CONNECTION_ID));
            destination.setType(Destination.DestinationType.HDFS);
            destination.setPath((String)destinationParams.get(HDFS_DESTINATION_PATH));
            destination.setFormat(CSV);
        } else if (destinationType.equals(DESTINATION_TYPE_SCP_OPTION)) {
            destination.setType(Destination.DestinationType.SCP);
            destination.setConnectionId((String)destinationParams.get(SCP_DESTINATION_CONNECTION_ID));
            destination.setPath((String)destinationParams.get(SCP_DESTINATION_PATH));
            destination.setFormat(CSV);
        }

        return destination;
    }

    public void setMode(Destination destination, Map<String, Object> params, String keyPrefix) {

        Map<String, Object> modeProp = (Map<String, Object>) params.getOrDefault(keyPrefix + ".mode", new HashMap());
        Map<String, Object> mode = (Map<String, Object>) modeProp.get(keyPrefix + ".modeMergeOption");
        if (mode != null) {
            String tableSchema = (String) mode.get(keyPrefix + ".table_schema");
            destination.setMode(MERGE);
            destination.setTableSchema(tableSchema);
        }
    }

}
