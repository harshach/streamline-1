package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;

public class AthenaxTopologyActionBuilder implements TopologyActionsBuilder<Map<String, Object>> {
    private TopologyActions athenaxTopologyActions;
    private Map<String, Object> conf;

    @Override
    public void init(Map<String, String> conf, TopologyActionsService topologyActionsService, Namespace namespace, Subject subject) {
        this.conf = new HashMap<>();
        athenaxTopologyActions = new AthenaxTopologyActionsImpl();
        athenaxTopologyActions.init(this.conf, topologyActionsService);
    }

    @Override
    public TopologyActions getTopologyActions() {
        return athenaxTopologyActions;
    }

    @Override
    public Map<String, Object> getConfig() {
        return conf;
    }

    @Override
    public void cleanup() {

    }
}
