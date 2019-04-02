package com.hortonworks.streamline.streams.actions.topology.state;

import com.hortonworks.streamline.streams.catalog.Template;

import java.util.HashMap;
import java.util.Map;

public class TopologyStateMachineFactory {
    private final Map<Template, TopologyStateMachine> topologyStateMachineMap;

    public TopologyStateMachineFactory() {
        this.topologyStateMachineMap = new HashMap<>();
    }

    public TopologyStateMachine getTopologyStateMachine(Template template) {
        TopologyStateMachine topologyStateMachine = topologyStateMachineMap.get(template);
        if (topologyStateMachine == null) {
            String topologyStateMachineClassName = template.getTopologyStateMachineClass();
            try {
                Class<TopologyStateMachine> topologyStateMachineClass = (Class<TopologyStateMachine>) Class.forName(topologyStateMachineClassName);
                topologyStateMachine = topologyStateMachineClass.newInstance();
                topologyStateMachineMap.put(template, topologyStateMachine);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                throw new RuntimeException("Can't initialize TopologyStateMachine instance - Class Name: " + topologyStateMachineClassName, e);
            }
        }
        return topologyStateMachine;
    }

}
