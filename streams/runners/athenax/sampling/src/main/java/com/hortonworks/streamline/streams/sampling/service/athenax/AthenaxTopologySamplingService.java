package com.hortonworks.streamline.streams.sampling.service.athenax;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import com.hortonworks.streamline.streams.sampling.service.TopologySampling;

import javax.security.auth.Subject;
import java.util.Map;

public class AthenaxTopologySamplingService implements TopologySampling {
	@Override
	public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService, Subject subject, Map<String, Object> conf) throws ConfigException  {

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
