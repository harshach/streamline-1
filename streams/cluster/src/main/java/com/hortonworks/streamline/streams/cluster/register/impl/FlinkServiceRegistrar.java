package com.hortonworks.streamline.streams.cluster.register.impl;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlinkServiceRegistrar extends AbstractServiceRegistrar {
	@Override
	protected String getServiceName() {
		return Constants.Flink.SERVICE_NAME;
	}

	@Override
	protected Map<Component, List<ComponentProcess>> createComponents(Config config, Map<String, String> flattenConfigMap) {
		Map<Component, List<ComponentProcess>> components = new HashMap<>();
		return components;
	}

	@Override
	protected List<ServiceConfiguration> createServiceConfigurations(Config config) {
		return Lists.newArrayList();
	}

	@Override
	protected boolean validateComponents(Map<Component, List<ComponentProcess>> components) {
		return true;
	}

	@Override
	protected boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
		return true;
	}

	@Override
	protected boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap) {
		return true;
	}
}
