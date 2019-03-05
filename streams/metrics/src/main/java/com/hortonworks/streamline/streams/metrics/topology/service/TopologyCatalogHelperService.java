package com.hortonworks.streamline.streams.metrics.topology.service;

import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.EngineMetricsBundle;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.catalog.TopologyRuntimeIdMap;
import com.hortonworks.streamline.streams.catalog.TopologyTask;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    public EngineMetricsBundle getEngineMetricsBundle(Long engineId) {
        return streamCatalogService.getEngineMetricsBundle(engineId);
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

    public Collection<? extends TopologyComponent> listStreamTopologyComponents(List<QueryParam> params) {
        Stream<? extends TopologyComponent> combinedStream = Stream.of(
                streamCatalogService.listTopologySources(params),
                streamCatalogService.listTopologyProcessors(params),
                streamCatalogService.listTopologySinks(params)
        ).flatMap(Collection::stream);
        return combinedStream.collect(Collectors.toList());
    }
}
