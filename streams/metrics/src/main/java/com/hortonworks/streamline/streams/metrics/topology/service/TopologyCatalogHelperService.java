package com.hortonworks.streamline.streams.metrics.topology.service;

import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyRuntimeIdMap;
import com.hortonworks.streamline.streams.catalog.TopologyTask;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.catalog.*;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        List<Long> namespaceIds = new ArrayList<>();
        namespaceIds.add(namespaceId);
        return streamCatalogService.getTopologyRuntimeIdMap(topologyId, namespaceIds);
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

    public ServiceConfiguration getServiceConfigurationByName(Long serviceId, String configurationName) {
        return environmentService.getServiceConfigurationByName(serviceId, configurationName);
    }

    public Namespace getNamespace(Long namespaceId) {
        return environmentService.getNamespace(namespaceId);
    }

    public Long getCurrentVersionId(Long topologyId) {
        return streamCatalogService.getCurrentVersionId(topologyId);
    }

    public Collection<TopologyTask> listTopologyTasks(List<QueryParam> params)  {
        return streamCatalogService.listTopologyTasks(params);
    }

}
