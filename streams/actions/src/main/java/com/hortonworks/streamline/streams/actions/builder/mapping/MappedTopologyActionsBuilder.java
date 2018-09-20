package com.hortonworks.streamline.streams.actions.builder.mapping;

import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;

public enum MappedTopologyActionsBuilder {
    STORM("com.hortonworks.streamline.streams.actions.storm.topology.StormTopologyActionsBuilder"),
    PIPER("com.hortonworks.streamline.streams.actions.piper.topology.PiperTopologyActionsBuilder");

    private final String className;

    MappedTopologyActionsBuilder(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public TopologyActionsBuilder instantiate(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<TopologyActionsBuilder> clazz = (Class<TopologyActionsBuilder>) Class.forName(className);
        return clazz.newInstance();
    }
}
