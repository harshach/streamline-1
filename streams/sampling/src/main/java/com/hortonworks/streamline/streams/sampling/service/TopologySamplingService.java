package com.hortonworks.streamline.streams.sampling.service;

import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;

import javax.security.auth.Subject;
import java.util.Map;

public class TopologySamplingService {
    private final TopologyCatalogHelperService topologyCatalogHelperService;
    private final TopologySamplingFactory topologySamplingFactory;
    private final Subject subject;

    public TopologySamplingService(TopologyCatalogHelperService topologyCatalogHelperService, Map<String, Object> config, Subject subject) {
        this.topologyCatalogHelperService = topologyCatalogHelperService;
        this.topologySamplingFactory = new TopologySamplingFactory(config);
        this.subject = subject;
    }

    public boolean enableSampling(Topology topology, int pct, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.enableSampling(topology, pct, asUser);
    }

    public boolean enableSampling(Topology topology, TopologyComponent component, int pct, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.enableSampling(topology, component, pct, asUser);
    }

    public boolean disableSampling(Topology topology, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.disableSampling(topology, asUser);
    }

    public boolean disableSampling(Topology topology, TopologyComponent component, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.disableSampling(topology, component, asUser);
    }

    public TopologySampling.SamplingStatus samplingStatus(Topology topology, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.getSamplingStatus(topology, asUser);
    }

    public TopologySampling.SamplingStatus samplingStatus(Topology topology, TopologyComponent component, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.getSamplingStatus(topology, component, asUser);
    }

    private TopologySampling getSamplingInstance(Topology topology) {
        Namespace namespace = topologyCatalogHelperService.getNamespace(topology.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
        }
        Engine engine = topologyCatalogHelperService.getEngine(topology.getEngineId());
        TopologySampling topologySampling = topologySamplingFactory.getTopologySampling(engine, namespace, topologyCatalogHelperService, subject);
        return topologySampling;
    }
}
