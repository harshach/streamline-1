package com.hortonworks.streamline.streams.metrics.topology.service;

import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyRuntimeIdMap;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.catalog.*;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.Collection;
import java.util.Optional;


public class TopologyCatalogHelperService {

    private final StreamCatalogService streamCatalogService;
    private final EnvironmentService environmentService;


    public TopologyCatalogHelperService(StreamCatalogService streamCatalogService, EnvironmentService environmentService) {
        this.streamCatalogService = streamCatalogService;
        this.environmentService = environmentService;
    }

    public Engine getEngine(Long engineId) {
        return streamCatalogService.getEngine(engineId);
    }

    public Topology getTopology(Long topologyId) {
        return streamCatalogService.getTopology(topologyId);
    }

    public TopologyRuntimeIdMap getTopologyRuntimeIdMap(Long topologyId, Long namespaceId) {
        return streamCatalogService.getTopologyRuntimeIdMap(topologyId, namespaceId);
    }

    public Service getFirstOccurenceServiceForNamespace(Namespace namespace, String serviceName) {
        return environmentService.getFirstOccurenceServiceForNamespace(namespace, serviceName);
    }

    public Optional<Component> getComponent(Service service, String componentName) {
        return environmentService.getComponent(service, componentName);
    }

    public Collection<ComponentProcess> listComponentProcesses(Long componentId) {
        return environmentService.listComponentProcesses(componentId);
    }

    public Namespace getNamespace(Long namespaceId) {
        return environmentService.getNamespace(namespaceId);
    }


}
