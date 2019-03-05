package com.hortonworks.streamline.streams.metrics.athenax.topology;

import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
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
import com.hortonworks.streamline.streams.metrics.M3MetricsQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AthenaxTopologyMetricsImpl implements TopologyMetrics {

    private static final String ATHENAX_METRIC_FRAMEWORK = "ATHENAX";
    private static final String ATHENAX_METRIC_PARAM_JOB_NAME = "jobName";
    private static final String ATHENAX_METRIC_PARAM_DC = "dc";
    private static final String ATHENAX_METRIC_PARAM_ENV = "env";

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
                     Map<String, Object> configuration, Subject subject) throws ConfigException {
        this.topologyCatalogHelperService = topologyCatalogHelperService;
        this.configMap = getConfigMap(namespace, engine);

        String host = configMap.get(AthenaxConstants.ATHENAX_SERVICE_HOST_KEY);
        String port = configMap.get(AthenaxConstants.ATHENAX_SERVICE_PORT_KEY);
        String athenaxVmApiRootUrl = "http://" + host + ":" + port;
        this.athenaXRestAPIClient = new AthenaXRestAPIClient(athenaxVmApiRootUrl, subject);
    }

    @Override
    public TopologyMetric getTopologyMetric(Topology topology, String asUser) throws IOException {
        Map<String, Object> metrics = new HashMap<>();

        String topologyRuntimeId = getTopologyRuntimeId(topology);
        if (topologyRuntimeId != null) {
            String yarnDataCenter = configMap.get(AthenaxConstants.ATHENAX_YARN_DATA_CENTER_KEY);
            String yarnCluster = configMap.get(AthenaxConstants.ATHENAX_YARN_CLUSTER_KEY);
            JobStatusRequest request = AthenaxUtils.extractJobStatusRequest(topologyRuntimeId, yarnDataCenter, yarnCluster);
            Map<String, String> statusMap = athenaXRestAPIClient.jobStatus(request);
            metrics.putAll(statusMap);
        }

        return new TopologyMetric(ATHENAX_METRIC_FRAMEWORK, topology.getName(), metrics);
    }

    @Override
    public Map<String, ComponentMetric> getComponentMetrics(Topology topologyLayout, String asUser) {
        Map<String, ComponentMetric> metricMap = new HashMap<>();
        return metricMap;
    }

    @Override
    public Map<String, Object> getExecution(Topology topology, String executionDate, String asUser) {
        throw new UnsupportedOperationException("getExecution not implemented");
    }

    @Override
    public Map<String, Object> getExecutions(Topology topology, Long from, Long to,
                                             Integer page, Integer pageSize, String asUser) {
        throw new UnsupportedOperationException("getExecutions not implemented");
    }

    @Override
    public Map<Long, Double> getTopologyTimeSeriesMetrics(Topology topology, String metricKeyName,
                                                          Map<String, String> metricQueryParams,
                                                          long from, long to, String asUser) {
        throw new UnsupportedOperationException("getTopologyTimeSeriesMetrics not implemented");
    }

    @Override
    public Map<Long, Map<Long, Double>> getComponentTimeSeriesMetrics(Topology topology, String metricKeyName,
                                                                      Map<String, String> metricQueryParams,
                                                                      long from, long to, String asUser) {
        throw new UnsupportedOperationException("getComponentTimeSeriesMetrics not implemented");
    }

    @Override
    public Map<Long, Double> getComponentTimeSeriesMetrics(Topology topology, TopologyComponent topoloyComponent,
                                                           String metricKeyName, Map<String, String> metricQueryParams,
                                                           long from, long to, String asUser) {
        throw new UnsupportedOperationException("getComponentTimeSeriesMetrics not implemented");
    }

    public Map<Long, Object> getTimeSeriesMetrics(Topology topology, String metricKeyName,
                                                  String metricQueryFormat, Map<String, String> clientMetricParams, long from, long to, String asUser) {

        Map<Long, Object> results = new HashMap<>();

        M3MetricsQuerier timeSeriesQuerier = (M3MetricsQuerier) this.timeSeriesQuerier;

        Map<String, String> metricParams = new HashMap<>();
        String jobName = topology.getName();
        String yarnDataCenter = configMap.get(AthenaxConstants.ATHENAX_YARN_DATA_CENTER_KEY);
        String yarnCluster = configMap.get(AthenaxConstants.ATHENAX_YARN_CLUSTER_KEY);
        metricParams.put(ATHENAX_METRIC_PARAM_JOB_NAME, jobName);
        metricParams.put(ATHENAX_METRIC_PARAM_DC, yarnDataCenter);
        metricParams.put(ATHENAX_METRIC_PARAM_ENV, yarnCluster);

        // merge (overwrite) params from client
        for (Map.Entry<String,String> entry : clientMetricParams.entrySet()) {
            metricParams.put(entry.getKey(), (entry.getValue()));
        }

        Map<Long, Double> metrics =
                timeSeriesQuerier.getMetrics(metricQueryFormat, metricParams, from, to, asUser);

        Collection<? extends TopologyComponent> components = getTopologyComponents(topology);

        if (components != null) {
            for (TopologyComponent component : components) {
                results.put(component.getId(), metrics);
            }
        }
        return results;
    }

    private Collection<? extends TopologyComponent> getTopologyComponents(Topology topology) {
        Long currentVersionId = topologyCatalogHelperService.getCurrentVersionId(topology.getId());

        List<QueryParam> queryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(topology.getId(), currentVersionId, null);

        return topologyCatalogHelperService.listStreamTopologyComponents(queryParams);
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

    private String getTopologyRuntimeId(Topology topology) {
        TopologyRuntimeIdMap topologyRuntimeIdMap = topologyCatalogHelperService.getTopologyRuntimeIdMap(
                topology.getId(), topology.getNamespaceId());
        if (topologyRuntimeIdMap == null) {
            throw new IllegalStateException("Cannot find topology runtime id map with id: " + topology.getId());
        }

        return topologyRuntimeIdMap.getApplicationId();
    }
}
