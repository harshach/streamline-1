package com.hortonworks.streamline.streams.cluster.bundle.impl;

public class PiperMetaStoreTaskBundleHintProvider extends PiperTaskBundleHintProvider {
    public String getConnectionType() { return "hive_metastore"; }

    public String getConnectionFieldName() { return "task_params.metastore_conn_id"; }
}
