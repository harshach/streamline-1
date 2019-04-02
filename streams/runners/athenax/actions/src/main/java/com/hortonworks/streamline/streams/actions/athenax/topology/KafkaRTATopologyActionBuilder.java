package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;

public class KafkaRTATopologyActionBuilder implements TopologyActionsBuilder<Map<String, Object>> {
    private TopologyActions kafkaRTATopologyActions;
    private Map<String, Object> conf;
    protected static final String UWORC_NAMESPACE_ID = "UWORC_NAMESPACE_ID";
    protected static final String UWORC_NAMESPACE_NAME = "UWORC_NAMESPACE_NAME";

    @Override
    public void init(Map<String, String> conf, Engine engine, TopologyActionsService topologyActionsService, EnvironmentService environmentService, Namespace namespace, Subject subject) {
        this.conf = new HashMap<>();
        this.conf.put(Constants.CONFIG_RTA_METADATA_SERVICE_URL, conf.get(Constants.CONFIG_RTA_METADATA_SERVICE_URL));
        this.conf.put(Constants.CONFIG_RTA_METADATA_SERVICE_MUTTLEY_NAME, conf.get(Constants.CONFIG_RTA_METADATA_SERVICE_MUTTLEY_NAME));
        this.conf.put(UWORC_NAMESPACE_ID, namespace.getId());
        this.conf.put(UWORC_NAMESPACE_NAME, namespace.getName());

        kafkaRTATopologyActions = new KafkaRTATopologyActionImpl();
        kafkaRTATopologyActions.init(this.conf, topologyActionsService, environmentService, subject);
    }

    @Override
    public TopologyActions getTopologyActions() {
        return kafkaRTATopologyActions;
    }

    @Override
    public Map<String, Object> getConfig() {
        return conf;
    }

    @Override
    public void cleanup() {

    }
}
