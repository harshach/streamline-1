package com.hortonworks.streamline.streams.metrics.athenax.topology;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;

public class KafkaRTATopologyMetricsImpl implements TopologyMetrics {
    private static final String KAFKA_RTA_METRIC_FRAMEWORK = "KAFKA_RTA";

    @Override
    public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService, Map<String, Object> configuration, Subject subject) throws ConfigException {

    }

    @Override
    public TopologyMetric getTopologyMetric(Topology topology, String asUser) {
        Map<String, Object> metrics = new HashMap<>();
        return new TopologyMetric(KAFKA_RTA_METRIC_FRAMEWORK, topology.getName(), metrics);
    }

    @Override
    public Map<String, ComponentMetric> getComponentMetrics(Topology topology, String asUser) {
        return null;
    }

    @Override
    public Map<String, Object> getExecution(Topology topology, String executionDate, String asUser) {
        return null;
    }

    @Override
    public Map<String, Object> getExecutions(Topology topology, Long from, Long to, Integer page, Integer pageSize, String asUser) {
        return null;
    }

    @Override
    public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {

    }

    @Override
    public TimeSeriesQuerier getTimeSeriesQuerier() {
        return null;
    }

    @Override
    public Map<Long, Double> getTopologyTimeSeriesMetrics(Topology topology, String metricKeyName, Map<String, String> metricQueryParams, long from, long to, String asUser) {
        return null;
    }

    @Override
    public Map<Long, Map<Long, Double>> getComponentTimeSeriesMetrics(Topology topology, String metricKeyName, Map<String, String> metricQueryParams, long from, long to, String asUser) {
        return null;
    }

    @Override
    public Map<Long, Double> getComponentTimeSeriesMetrics(Topology topology, TopologyComponent topologyComponent, String metricKeyName, Map<String, String> metricQueryParams, long from, long to, String asUser) {
        return null;
    }

    @Override
    public TimeSeriesComponentMetric getTopologyStats(TopologyLayout topology, long from, long to, String asUser) {
        return null;
    }

    @Override
    public Map<Long, Double> getCompleteLatency(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return null;
    }

    @Override
    public Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return null;
    }

    @Override
    public TimeSeriesComponentMetric getComponentStats(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return null;
    }
}
