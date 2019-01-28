package com.hortonworks.streamline.streams.actions.topology.state;

import com.hortonworks.streamline.streams.catalog.Engine;

import java.util.HashMap;
import java.util.Map;

public class TopologyStateMachineFactory {
    private final Map<Engine, TopologyStateMachine> topologyStateMachineMap;

    public TopologyStateMachineFactory() {
        this.topologyStateMachineMap = new HashMap<>();
    }

    public TopologyStateMachine getTopologyStateMachine(Engine engine) {
        TopologyStateMachine topologyStateMachine = topologyStateMachineMap.get(engine);
        if (topologyStateMachine == null) {
            String topologyStateMachineClassName = engine.getTopologyStateMachineClass();
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

}
