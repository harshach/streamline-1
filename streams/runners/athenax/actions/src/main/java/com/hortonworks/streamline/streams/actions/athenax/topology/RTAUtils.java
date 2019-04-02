package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.layout.component.impl.RTASink;
import com.hortonworks.streamline.streams.registry.table.RTACreateTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTADeployTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTAQueryTypes;
import com.hortonworks.streamline.streams.registry.table.RTATableField;
import com.hortonworks.streamline.streams.registry.table.RTATableMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RTAUtils {
    public static RTACreateTableRequest extractRTACreateTableRequest(RTASink rtaSink, String runAsUser) {
        RTACreateTableRequest request = new RTACreateTableRequest();

        Config rtaSinkConfig = rtaSink.getConfig();

        // TODO: Change to use email in runAsUser when available
        request.setOwner(runAsUser + "@uber.com");
        request.setName(rtaSinkConfig.get(RTAConstants.TABLE_NAME));
        request.setRtaTableMetadata(extractRTATableMetadata(rtaSink));

        List<RTATableField> rtaTableFields = new ArrayList<>();
        List<Map<String, Object>> tableFieldConfigs = rtaSinkConfig.getAny(RTAConstants.TABLE_FIELDS);
        for (Map<String, Object> fieldConfig : tableFieldConfigs) {
            RTATableField rtaTableField = new RTATableField();

            rtaTableField.setType((String) fieldConfig.get(RTAConstants.TYPE));
            rtaTableField.setName((String) fieldConfig.get(RTAConstants.NAME));
            rtaTableField.setUberLogicalType((String) fieldConfig.get(RTAConstants.UBER_LOGICAL_TYPE));
            rtaTableField.setCardinality((String) fieldConfig.get(RTAConstants.CARDINALITY));
            rtaTableField.setColumnType((String) fieldConfig.get(RTAConstants.COLUMN_TYPE));
            rtaTableField.setDoc((String) fieldConfig.get(RTAConstants.DOC));

            rtaTableFields.add(rtaTableField);
        }
        request.setFields(rtaTableFields);

        return request;
    }

    private static RTATableMetadata extractRTATableMetadata(RTASink rtaSink) {
        RTATableMetadata metaData = new RTATableMetadata();

        Config rtaSinkConfig = rtaSink.getConfig();

        List<String> primaryKeys = new ArrayList<>();
        List<Map<String, Object>> tableFieldConfigs = rtaSinkConfig.getAny(RTAConstants.TABLE_FIELDS);
        for (Map<String, Object> fieldConfig : tableFieldConfigs) {
            if ((boolean) fieldConfig.get(RTAConstants.IS_PRIMARY_KEY)) {
                primaryKeys.add((String) fieldConfig.get(RTAConstants.NAME));
            }
        }
        metaData.setPrimaryKeys(primaryKeys);

        metaData.setIngestionRate(rtaSinkConfig.getAny(RTAConstants.INGESTION_RATE));
        metaData.setRetentionDays(rtaSinkConfig.getAny(RTAConstants.RETENTION_DAYS));

        List<String> queryTypes = new ArrayList<>();
        for (RTAQueryTypes rtaQueryTypes : RTAQueryTypes.values()) {
            if (rtaSinkConfig.getAny(rtaQueryTypes.getUiFieldName())) {
                queryTypes.add(rtaQueryTypes.getRtaQueryTypeName());
            }
        }
        metaData.setQueryTypes(queryTypes);

        // source topic for RTA ingestion, which is the topic defined in RTA sink
        String rtaSourceTopicName = rtaSinkConfig.get(RTAConstants.TOPIC);
        metaData.setSourceName(rtaSourceTopicName);

        return metaData;
    }

    public static RTADeployTableRequest extractRTADeployTableRequest(RTASink rtaSink) {
        RTADeployTableRequest request = new RTADeployTableRequest();
        String clusterName = rtaSink.getConfig().get("clusters");
        request.setKafkaCluster(clusterName);
        return request;
    }
}
