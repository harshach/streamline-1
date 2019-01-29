package com.hortonworks.streamline.streams.metrics.athenax.topology;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyRuntimeIdMap;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.common.athenax.AthenaXRestAPIClient;
import com.hortonworks.streamline.streams.common.athenax.AthenaxConstants;
import com.hortonworks.streamline.streams.common.athenax.AthenaxUtils;
import com.hortonworks.streamline.streams.common.athenax.entity.JobStatusRequest;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AthenaxTopologyMetricsImpl implements TopologyMetrics {

    private static final String ATHENAX_METRIC_FRAMEWORK = "ATHENAX";

    private Map<String, String> configMap;
    private TimeSeriesQuerier timeSeriesQuerier;
    private AthenaXRestAPIClient athenaXRestAPIClient;
    private TopologyCatalogHelperService topologyCatalogHelperService;

    @Override
    public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {
        this.timeSeriesQuerier = timeSeriesQuerier;
    }

    @Override
    public TimeSeriesComponentMetric getTopologyStats(TopologyLayout topologyLayout, long from, long to, String asUser) {
        return null;
    }

    @Override
    public Map<Long, Double> getCompleteLatency(TopologyLayout topologyLayout, Component component, long from, long to, String asUser) {
        return null;
    }

    @Override
    public Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topologyLayout, Component component, long from, long to, String asUser) {
        return null;
    }

    @Override
    public TimeSeriesComponentMetric getComponentStats(TopologyLayout topologyLayout, Component component, long from, long to, String asUser) {
        return null;
    }

    @Override
    public TimeSeriesQuerier getTimeSeriesQuerier() {
        return this.timeSeriesQuerier;
    }

    @Override
    public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
                     Subject subject) throws ConfigException {
        this.topologyCatalogHelperService = topologyCatalogHelperService;
        this.configMap = getConfigMap(namespace, engine);

        String host = configMap.get(AthenaxConstants.ATHENAX_SERVICE_HOST_KEY);
        String port = configMap.get(AthenaxConstants.ATHENAX_SERVICE_PORT_KEY);
        String athenaxVmApiRootUrl = "http://" + host + ":" + port;
        this.athenaXRestAPIClient = new AthenaXRestAPIClient(athenaxVmApiRootUrl, subject);
    }

    @Override
    public TopologyMetric getTopologyMetric(TopologyLayout topologyLayout, String asUser) throws IOException {
        Map<String, Object> metrics = new HashMap<>();

        String topologyRuntimeId = getTopologyRuntimeId(topologyLayout);
        if (topologyRuntimeId != null) {
            String yarnDataCenter = configMap.get(AthenaxConstants.ATHENAX_YARN_DATA_CENTER_KEY);
            String yarnCluster = configMap.get(AthenaxConstants.ATHENAX_YARN_CLUSTER_KEY);
            JobStatusRequest request = AthenaxUtils.extractJobStatusRequest(topologyRuntimeId, yarnDataCenter, yarnCluster);
            Map<String, String> statusMap = athenaXRestAPIClient.jobStatus(request);
            metrics.putAll(statusMap);
        }

        return new TopologyMetric(ATHENAX_METRIC_FRAMEWORK, topologyLayout.getName(), metrics);
    }

    @Override
    public Map<String, ComponentMetric> getMetricsForTopology(TopologyLayout topologyLayout, String asUser) {
        Map<String, ComponentMetric> metricMap = new HashMap<>();
        return metricMap;
    }

    private Map<String, String> getConfigMap(Namespace namespace, Engine engine) throws ConfigException {
        Service service = topologyCatalogHelperService.getFirstOccurenceServiceForNamespace(namespace, engine.getName());

        if (service == null) {
            throw new ConfigException("Service " + AthenaxConstants.ATHENAX_SERVICE_NAME +
                    " is not associated to the namespace " + namespace.getName() + "(" + namespace.getId() + ")");
        }

        ServiceConfiguration serviceConfiguration = topologyCatalogHelperService.getServiceConfigurationByName(
                service.getId(), AthenaxConstants.ATHENAX_SERVICE_CONFIG_NAME);

        Map<String, String> configMap;
        try {
            configMap = serviceConfiguration.getConfigurationMap();
        } catch (IOException e) {
            throw new ConfigException("Cannot load config map for service " + AthenaxConstants.ATHENAX_SERVICE_NAME);
        }

        if (configMap == null) {
            throw new ConfigException("Cannot find config map for service " + AthenaxConstants.ATHENAX_SERVICE_NAME);
        }
        return configMap;
    }

    private String getTopologyRuntimeId(TopologyLayout topologyLayout) {
        Topology topology = topologyCatalogHelperService.getTopology(topologyLayout.getId());
        if (topology == null) {
            throw new IllegalStateException("Cannot find topology with id: " + topologyLayout.getId());
        }

        TopologyRuntimeIdMap topologyRuntimeIdMap = topologyCatalogHelperService.getTopologyRuntimeIdMap(
                topology.getId(), topology.getNamespaceId());
        if (topologyRuntimeIdMap == null) {
            throw new IllegalStateException("Cannot find topology runtime id map with id: " + topology.getId());
        }

        return topologyRuntimeIdMap.getApplicationId();
    }
}
