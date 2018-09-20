package com.hortonworks.streamline.streams.actions.piper.topology;

import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;

/**
 * ToplogyActionsBuilder for PIPER implementation
 */
public class PiperTopologyActionsBuilder implements TopologyActionsBuilder<Map<String, Object>> {

    private Map<String, String> streamlineConf;
    private TopologyActionsService topologyActionsService;
    private TopologyActions piperTopologyActions;
    private Map<String, Object> conf;

    @Override
    public void init(Map<String, String> streamlineConf, TopologyActionsService topologyActionsService, Namespace namespace, Subject subject) {
        this.conf = new HashMap<>();
        this.topologyActionsService = topologyActionsService;
        this.streamlineConf = streamlineConf;
        buildPiperTopologyActionsConfigMap(namespace, subject);
        piperTopologyActions = new PiperTopologyActionsImpl();
        piperTopologyActions.init(this.conf, topologyActionsService);
    }

    private void buildPiperTopologyActionsConfigMap(Namespace namespace, Subject subject) {
        // TODO: Configure things like Piper URL, etc
    }

    @Override
    public TopologyActions getTopologyActions() {
        return piperTopologyActions;
    }

    @Override
    public Map<String, Object> getConfig() {
        return conf;
    }

    @Override
    public void cleanup() {

    }
}
