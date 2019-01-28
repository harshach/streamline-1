package com.hortonworks.streamline.streams.actions.builder;

import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.Engine;

import javax.security.auth.Subject;
import java.util.Map;

public interface TopologyActionsBuilder<T> {

    /**
     * initialize the configs based on the namespace
     */
    void init(Map<String, String> conf, Engine engine, TopologyActionsService topologyActionsService,
              Namespace namespace, Subject subject) throws Exception;

    TopologyActions getTopologyActions();

    T getConfig();

    void cleanup();
}
