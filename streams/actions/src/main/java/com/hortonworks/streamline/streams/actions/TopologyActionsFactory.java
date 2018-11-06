package com.hortonworks.streamline.streams.actions;

import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import joptsimple.internal.Strings;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TopologyActionsFactory {
    private static final String ENGINE_CONFIGURATION = "engineConfiguration";
    private static final String ENGINE_NAME = "engineName";
    private static final String PROPERTIES = "properties";
    private static final String TOPOLOGY_ACTIONS_CLASS = "topologyActionsClass";

    Map<String, Object> config;
    private final Map<Engine, Map<Namespace, TopologyActionsBuilder>> topologyActionsBuilderMap;


    public TopologyActionsFactory(Map<String, Object> config) {
        this.config = config;
        this.topologyActionsBuilderMap = new HashMap<>();
    }

    public TopologyActionsBuilder getTopologyActionsBuilder(Engine engine, Namespace namespace, TopologyActionsService topologyActionsService,
                                                            Map<String, String> streamlineConfig, Subject subject) {
        topologyActionsBuilderMap.putIfAbsent(engine,
                new HashMap<>());
        Map<Namespace, TopologyActionsBuilder> topologyActionsMap = topologyActionsBuilderMap.get(engine);
        TopologyActionsBuilder topologyActionsBuilder = topologyActionsMap.get(namespace);
        String className = Strings.EMPTY;
        if (topologyActionsBuilder == null) {
            try {
                String topologyActionsBuilderClazz = getConfiguredClass(engine, TOPOLOGY_ACTIONS_CLASS);
                topologyActionsBuilder = instantiateTopologyActionsBuilder(topologyActionsBuilderClazz);
                topologyActionsBuilder.init(streamlineConfig, topologyActionsService, namespace, subject);
                topologyActionsMap.put(namespace, topologyActionsBuilder);
                topologyActionsBuilderMap.put(engine, topologyActionsMap);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                throw new RuntimeException("Can't initialize Topology actions instance - Class Name: " + className, e);
            }
        }
        return topologyActionsBuilder;
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

    private TopologyActionsBuilder instantiateTopologyActionsBuilder(String className) throws
            ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<TopologyActionsBuilder> clazz = (Class<TopologyActionsBuilder>) Class.forName(className);
        return clazz.newInstance();
    }
}
