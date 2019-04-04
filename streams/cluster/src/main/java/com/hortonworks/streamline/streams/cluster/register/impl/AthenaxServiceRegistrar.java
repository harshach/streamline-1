package com.hortonworks.streamline.streams.cluster.register.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AthenaxServiceRegistrar extends AbstractServiceRegistrar {

    public static final String ATHENAX_SERVICE_HOST_KEY = "athenax.service.host";
    public static final String ATHENAX_SERVICE_PORT_KEY= "athenax.service.port";
    public static final String ATHENAX_YARN_DATA_CENTER_KEY = "athenax.yarn.dataCenter";
    public static final String ATHENAX_YARN_CLUSTER_KEY = "athenax.yarn.cluster";
    public static final String ATHENAX_SERVICE_MUTTLEY_NAME = "athenax.service.muttleyName";
    public static final String SERVICE_NAME = "athenax";
    public static final String CONF_TYPE_PROPERTIES = "properties";

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected Map<Component, List<ComponentProcess>> createComponents(Config config, Map<String, String> flattenConfigMap) {
        // no component to register
        return Collections.emptyMap();
    }

    @Override
    protected List<ServiceConfiguration> createServiceConfigurations(Config config) {
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setName(CONF_TYPE_PROPERTIES);

        Map<String, String> confMap = new HashMap<>();
        confMap.put(ATHENAX_SERVICE_HOST_KEY, config.get(ATHENAX_SERVICE_HOST_KEY));
        confMap.put(ATHENAX_SERVICE_PORT_KEY, String.valueOf((Integer) config.getAny(ATHENAX_SERVICE_PORT_KEY)));
        confMap.put(ATHENAX_YARN_DATA_CENTER_KEY, config.get(ATHENAX_YARN_DATA_CENTER_KEY));
        confMap.put(ATHENAX_YARN_CLUSTER_KEY, config.get(ATHENAX_YARN_CLUSTER_KEY));
        confMap.put(ATHENAX_SERVICE_MUTTLEY_NAME, config.get(ATHENAX_SERVICE_MUTTLEY_NAME));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(confMap);
            serviceConfiguration.setConfiguration(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return Collections.singletonList(serviceConfiguration);
    }

    @Override
    protected boolean validateComponents(Map<Component, List<ComponentProcess>> components) {
        // no need to check components
        return true;
    }

    @Override
    protected boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        //TODO: Add separate constants
        return serviceConfigurations.stream()
                .anyMatch(config -> config.getName().equals(CONF_TYPE_PROPERTIES));
    }

    @Override
    protected boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap) {
        return true;
    }
}
