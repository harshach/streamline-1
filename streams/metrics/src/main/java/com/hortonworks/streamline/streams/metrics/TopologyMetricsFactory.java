package com.hortonworks.streamline.streams.metrics;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.EngineMetricsBundle;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import joptsimple.internal.Strings;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;

public class TopologyMetricsFactory {
    Map<String, Object> config;
    private final Map<Engine, Map<Namespace, TopologyMetrics>> topologyMetricsMap;
    public static final String METRICS_UI_SPEC = "metricsUISpec";


    public TopologyMetricsFactory(Map<String, Object> config) {
        this.config = config;
        this.topologyMetricsMap = new HashMap<>();
    }

    public TopologyMetrics getTopologyMetrics(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
                                              Subject subject) {
        topologyMetricsMap.putIfAbsent(engine, new HashMap<>());
        Map<Namespace, TopologyMetrics> metricsMap = topologyMetricsMap.get(engine);
        TopologyMetrics topologyMetrics = metricsMap.get(namespace);
        String topologyMetricsClazz = Strings.EMPTY;
        if (topologyMetrics == null) {
            try {
                topologyMetricsClazz = engine.getTopologyStatusMetricsClass();
                if (topologyMetricsClazz != null && !topologyMetricsClazz.isEmpty()) {
                    Map<String, Object> configuration = buildConfiguration(engine, namespace, topologyCatalogHelperService);
                    topologyMetrics = instantiateTopologyMetrics(topologyMetricsClazz);
                    topologyMetrics.init(engine, namespace, topologyCatalogHelperService, configuration, subject);
                    String topologyTimeseriesClazz = engine.getTopologyTimeseriesMetricsClass();
                    TimeSeriesQuerier timeSeriesQuerier = instantiateTimeSeriesQuerier(topologyTimeseriesClazz);
                    timeSeriesQuerier.init(engine, namespace, topologyCatalogHelperService, subject, config);
                    topologyMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
                    metricsMap.put(namespace, topologyMetrics);
                    topologyMetricsMap.put(engine, metricsMap);
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException  | ConfigException e) {
                throw new RuntimeException("Can't initialize Topology Metrics instance - Class Name: " + topologyMetricsClazz, e);
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

    private Map<String, Object> buildConfiguration(Engine engine, Namespace namespace,
                                                   TopologyCatalogHelperService topologyCatalogHelperService) {
        Map<String, Object> config = new HashMap<>();

        EngineMetricsBundle engineMetricsBundle = topologyCatalogHelperService.getEngineMetricsBundle(engine.getId());

        config.put(METRICS_UI_SPEC, engineMetricsBundle.getMetricsUISpec());

        return config;
    }


}
