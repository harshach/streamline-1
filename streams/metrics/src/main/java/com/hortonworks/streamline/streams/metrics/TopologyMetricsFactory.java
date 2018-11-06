package com.hortonworks.streamline.streams.metrics;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import joptsimple.internal.Strings;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopologyMetricsFactory {

    private static final String ENGINE_CONFIGURATION = "engineConfiguration";
    private static final String ENGINE_NAME = "engineName";
    private static final String PROPERTIES = "properties";
    private static final String TOPOLOGY_METRICS_CLASS = "topologyMetricsClass";
    private static final String TIMESERIES_METRICS_CLASS = "timeseriesMetricsClass";

    Map<String, Object> config;
    private final Map<Engine, Map<Namespace, TopologyMetrics>> topologyMetricsMap;


    public TopologyMetricsFactory(Map<String, Object> config) {
        this.config = config;
        this.topologyMetricsMap = new HashMap<>();
    }

    public TopologyMetrics getTopologyMetrics(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
                                              Subject subject) {

        topologyMetricsMap.putIfAbsent(engine,
                new HashMap<>());
        Map<Namespace, TopologyMetrics> metricsMap = topologyMetricsMap.get(engine);
        TopologyMetrics topologyMetrics = metricsMap.get(namespace);
        String className = Strings.EMPTY;
        if (topologyMetrics == null) {
            try {
                String topologyMetricsClazz = getConfiguredClass(engine, TOPOLOGY_METRICS_CLASS);
                topologyMetrics = instantiateTopologyMetrics(topologyMetricsClazz);
                topologyMetrics.init(engine, namespace, topologyCatalogHelperService, subject, config);
                String topologyTimeseriesClazz = getConfiguredClass(engine, TOPOLOGY_METRICS_CLASS);
                TimeSeriesQuerier timeSeriesQuerier = instantiateTimeSeriesQuerier(topologyTimeseriesClazz);
                timeSeriesQuerier.init(engine, namespace, topologyCatalogHelperService, subject, config);
                topologyMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
                metricsMap.put(namespace, topologyMetrics);
                topologyMetricsMap.put(engine, metricsMap);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException  | ConfigException e) {
                throw new RuntimeException("Can't initialize Topology actions instance - Class Name: " + className, e);
            }
        }
        return topologyMetrics;
    }


    private TopologyMetrics instantiateTopologyMetrics(String className) throws
            ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<TopologyMetrics> clazz = (Class<TopologyMetrics>) Class.forName(className);
        return clazz.newInstance();
    }

    private TimeSeriesQuerier instantiateTimeSeriesQuerier(String className) throws
            ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<TimeSeriesQuerier> clazz = (Class<TimeSeriesQuerier>) Class.forName(className);
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
