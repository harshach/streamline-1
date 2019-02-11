package com.hortonworks.streamline.streams.cluster.bundle.impl;

public class PiperSparkTaskBundleHintProvider extends PiperTaskBundleHintProvider {
    public String getConnectionType() { return "spark"; }

    public String getConnectionFieldName() { return "task_params.spark_conn_id"; }
}
