package com.hortonworks.streamline.streams.sampling.service;

import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;

import javax.security.auth.Subject;
import java.util.Map;

public interface TopologySampling {
    void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService, Subject subject, Map<String, Object> conf);

    boolean enableSampling(Topology topology, int pct, String asUser);

    boolean enableSampling(Topology topology, TopologyComponent component, int pct, String asUser);

    boolean disableSampling(Topology topology, String asUser);

    boolean disableSampling(Topology topology, TopologyComponent component, String asUser);

    SamplingStatus getSamplingStatus(Topology topology, String asUser);

    SamplingStatus getSamplingStatus(Topology topology, TopologyComponent component, String asUser);

    interface SamplingStatus {
        Boolean getEnabled();
        Integer getPct();
    }
}
