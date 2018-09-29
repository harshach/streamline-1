package com.hortonworks.streamline.streams.cluster.bundle.impl;

public class PiperHiveTaskBundleHintProvider extends PiperTaskBundleHintProvider {
    public String getConnectionType() { return "hive_cli"; }

    public String getConnectionFieldName() { return "hive_cli_conn_id"; }
}
