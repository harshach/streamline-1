package com.hortonworks.streamline.streams.metrics.flink.topology;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;

import java.util.Map;

public class FlinkTopologyMetricsImpl implements TopologyMetrics {
	@Override
	public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {

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

	@Override
	public TimeSeriesQuerier getTimeSeriesQuerier() {
		return null;
	}

	@Override
	public void init(Map<String, Object> conf) throws ConfigException {

	}

	@Override
	public TopologyMetric getTopologyMetric(TopologyLayout topology, String asUser) {
		return null;
	}

	@Override
	public Map<String, ComponentMetric> getMetricsForTopology(TopologyLayout topology, String asUser) {
		return null;
	}
}
