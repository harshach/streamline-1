package com.hortonworks.streamline.streams.common.athenax;

public class AthenaxConstants {
    public static final String YARN_APPLICATION_STATE_RUNNING = "RUNNING";
    public static final String YARN_APPLICATION_STATE_FINISHED = "FINISHED";
    public static final String YARN_APPLICATION_STATE_FAILED = "FAILED";
    public static final String YARN_APPLICATION_STATE_KILLED = "KILLED";
    public static final String YARN_APPLICATION_STATE_UNKNOWN = "UNKNOWN";
    public static final String ATHENAX_RUNTIME_STATUS_ENABLED = "enabled";
    public static final String ATHENAX_RUNTIME_STATUS_PAUSED = "paused";
    public static final String ATHENAX_RUNTIME_STATUS_INACTIVE = "inactive";
    public static final String ATHENAX_RUNTIME_STATUS_UNKNOWN = "unknown";

    // TODO: consolidate the following with the ones in com.hortonworks.streamline.streams.cluster.register.impl.AthenaxServiceRegistrar
    public static final String ATHENAX_SERVICE_NAME = "ATHENAX";
    public static final String ATHENAX_SERVICE_HOST_KEY = "athenax.service.host";
    public static final String ATHENAX_SERVICE_PORT_KEY = "athenax.service.port";
    public static final String ATHENAX_SERVICE_ROOT_URL_KEY = "athenax.service.rootUrl";
    public static final String ATHENAX_YARN_DATA_CENTER_KEY = "athenax.yarn.dataCenter";
    public static final String ATHENAX_YARN_CLUSTER_KEY = "athenax.yarn.cluster";
    public static final String ATHENAX_SERVICE_CONFIG_NAME = "properties";
}
