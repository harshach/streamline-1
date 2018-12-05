package com.hortonworks.streamline.streams.actions.topology.state;

import com.hortonworks.streamline.streams.catalog.Engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopologyStateMachineFactory {
    private static final String ENGINE_CONFIGURATION = "engineConfiguration";
    private static final String ENGINE_NAME = "engineName";
    private static final String PROPERTIES = "properties";
    private static final String TOPOLOGY_STATE_MACHINE_CLASS = "topologyStateMachineClass";

    private final Map<String, Object> config;
    private final Map<Engine, TopologyStateMachine> topologyStateMachineMap;

    public TopologyStateMachineFactory(Map<String, Object> config) {
        this.config = config;
        this.topologyStateMachineMap = new HashMap<>();
    }

    public TopologyStateMachine getTopologyStateMachine(Engine engine) {
        TopologyStateMachine topologyStateMachine = topologyStateMachineMap.get(engine);
        if (topologyStateMachine == null) {
            String topologyStateMachineClassName = getConfiguredClass(engine, TOPOLOGY_STATE_MACHINE_CLASS);
            try {
                Class<TopologyStateMachine> topologyStateMachineClass = (Class<TopologyStateMachine>) Class.forName(topologyStateMachineClassName);
                topologyStateMachine = topologyStateMachineClass.newInstance();
                topologyStateMachineMap.put(engine, topologyStateMachine);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                throw new RuntimeException("Can't initialize TopologyStateMachine instance - Class Name: " + topologyStateMachineClassName, e);
            }
        }
        return topologyStateMachine;
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
