package com.hortonworks.streamline.streams.actions;

import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Template;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import joptsimple.internal.Strings;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;


public class TopologyActionsFactory {
    private final Map<Template, Map<Namespace, TopologyActionsBuilder>> topologyActionsBuilderMap;


    public TopologyActionsFactory() {
        this.topologyActionsBuilderMap = new HashMap<>();
    }

    public TopologyActionsBuilder getTopologyActionsBuilder(Engine engine, Namespace namespace, Template template, TopologyActionsService topologyActionsService,
                                                            EnvironmentService envinronmentService, Map<String, String> streamlineConfig, Subject subject) throws Exception {
        topologyActionsBuilderMap.putIfAbsent(template,
                new HashMap<>());
        Map<Namespace, TopologyActionsBuilder> topologyActionsMap = topologyActionsBuilderMap.get(template);
        TopologyActionsBuilder topologyActionsBuilder = topologyActionsMap.get(namespace);
        String className = Strings.EMPTY;
        if (topologyActionsBuilder == null) {
            try {
                String topologyActionsBuilderClazz = template.getTopologyActionClass();
                topologyActionsBuilder = instantiateTopologyActionsBuilder(topologyActionsBuilderClazz);
                topologyActionsBuilder.init(streamlineConfig, engine, topologyActionsService, envinronmentService, namespace, subject);
                topologyActionsMap.put(namespace, topologyActionsBuilder);
                topologyActionsBuilderMap.put(template, topologyActionsMap);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                throw new RuntimeException("Can't initialize Topology actions instance - Class Name: " + className, e);
            }
        }
        return topologyActionsBuilder;
    }


    private TopologyActionsBuilder instantiateTopologyActionsBuilder(String className) throws
            ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<TopologyActionsBuilder> clazz = (Class<TopologyActionsBuilder>) Class.forName(className);
        return clazz.newInstance();
    }
}
