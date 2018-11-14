package com.hortonworks.streamline.streams.sampling.service;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import joptsimple.internal.Strings;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopologySamplingFactory {

    private static final String ENGINE_CONFIGURATION = "engineConfiguration";
    private static final String ENGINE_NAME = "engineName";
    private static final String PROPERTIES = "properties";
    private static final String TOPOLOGY_SAMPLING_CLASS = "topologySamplingClass";

    Map<String, Object> config;
    private final Map<Engine, Map<Namespace, TopologySampling>> topologySamplingMap;

    public TopologySamplingFactory(Map<String, Object> config) {
        this.config = config;
        this.topologySamplingMap = new HashMap<>();
    }

    public TopologySampling getTopologySampling(Engine engine, Namespace namespace,
                                                TopologyCatalogHelperService topologyCatalogHelperService, Subject subject) {
        topologySamplingMap.putIfAbsent(engine,
                new HashMap<>());
        Map<Namespace, TopologySampling> samplingMap = topologySamplingMap.get(engine);
        TopologySampling topologySampling = samplingMap.get(namespace);
        String className = Strings.EMPTY;
        if (topologySampling == null) {
            try {
                String topologySamplingClazz = getConfiguredClass(engine, TOPOLOGY_SAMPLING_CLASS);
                topologySampling = instantiateTopologySampling(topologySamplingClazz);
                topologySampling.init(engine, namespace, topologyCatalogHelperService, subject, config);
                samplingMap.put(namespace, topologySampling);
                topologySamplingMap.put(engine, samplingMap);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ConfigException e) {
                throw new RuntimeException("Can't initialize Topology actions instance - Class Name: " + className, e);
            }
        }
        return topologySampling;
    }

    private TopologySampling instantiateTopologySampling(String className) throws
            ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<TopologySampling> clazz = (Class<TopologySampling>) Class.forName(className);
        return clazz.newInstance();
    }

    private String getConfiguredClass(Engine engine, String implClass) {
        List<Map<String, Object>> engineConfigurations = (List<Map<String, Object>>) config.get(ENGINE_CONFIGURATION);
        Map<String, String> engineClassConfigs = new HashMap<>();
        for (Map<String, Object> engineConfig: engineConfigurations) {
            String engineName = (String) engineConfig.get(ENGINE_NAME);
            if (engineName.equalsIgnoreCase(engine.getName()))
                engineClassConfigs = (Map<String, String>) engineConfig.get(PROPERTIES);
        }
        return engineClassConfigs.get(implClass);
    }
}
