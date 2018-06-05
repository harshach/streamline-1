package com.hortonworks.streamline.streams.sampling.service.flink;

import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.sampling.service.TopologySampling;

import java.util.Map;

public class FlinkTopologySamplingService implements TopologySampling {
	@Override
	public void init(Map<String, Object> conf) {

	}

	@Override
	public boolean enableSampling(Topology topology, int pct, String asUser) {
		return false;
	}

	@Override
	public boolean enableSampling(Topology topology, TopologyComponent component, int pct, String asUser) {
		return false;
	}

	@Override
	public boolean disableSampling(Topology topology, String asUser) {
		return false;
	}

	@Override
	public boolean disableSampling(Topology topology, TopologyComponent component, String asUser) {
		return false;
	}

	@Override
	public SamplingStatus getSamplingStatus(Topology topology, String asUser) {
		return null;
	}

	@Override
	public SamplingStatus getSamplingStatus(Topology topology, TopologyComponent component, String asUser) {
		return null;
	}
}
