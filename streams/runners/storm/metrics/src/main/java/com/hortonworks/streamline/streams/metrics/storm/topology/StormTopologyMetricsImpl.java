/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.metrics.storm.topology;

import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIClient;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static com.hortonworks.streamline.streams.storm.common.StormRestAPIConstant.*;

/**
 * Storm implementation of the TopologyMetrics interface
 */
public class StormTopologyMetricsImpl implements TopologyMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologyMetricsImpl.class);
    public static final String COMPONENT_NAME_STORM_UI_SERVER = ComponentPropertyPattern.STORM_UI_SERVER.name();

    private static final String FRAMEWORK = "STORM";
    private static final int MAX_SIZE_TOPOLOGY_CACHE = 10;
    private static final int MAX_SIZE_COMPONENT_CACHE = 50;
    private static final int CACHE_DURATION_SECS = 5;
    private static final int FORK_JOIN_POOL_PARALLELISM = 50;

    // shared across the metrics instances
    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);
    private TopologyCatalogHelperService topologyCatalogHelperService;
    private StormRestAPIClient client;
    private Subject subject;
    private Engine engine;
    private Namespace namespace;
    private TopologyTimeSeriesMetrics timeSeriesMetrics;

    private LoadingCache<Pair<String, String>, Map<String, ?>> topologyRetrieveCache;
    private LoadingCache<Pair<Pair<String, String>, String>, Map<String, ?>> componentRetrieveCache;

    public StormTopologyMetricsImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
                     Subject subject, Map<String, Object> conf) throws ConfigException {

        this.subject = subject;
        this.namespace = namespace;
        this.engine = engine;
        this.topologyCatalogHelperService = topologyCatalogHelperService;

        String stormApiRootUrl = buildStormRestApiRootUrl();
        Client restClient = ClientBuilder.newClient(new ClientConfig());
        this.client = new StormRestAPIClient(restClient, stormApiRootUrl, subject);
        timeSeriesMetrics = new StormTopologyTimeSeriesMetricsImpl(client);
        topologyRetrieveCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_SIZE_TOPOLOGY_CACHE)
                .expireAfterWrite(CACHE_DURATION_SECS, TimeUnit.SECONDS)
                .build(new CacheLoader<Pair<String, String>, Map<String, ?>>() {
                    @Override
                    public Map<String, ?> load(Pair<String, String> stormTopologyIdAndAsUser) {
                        String stormTopologyId = stormTopologyIdAndAsUser.getLeft();
                        String asUser = stormTopologyIdAndAsUser.getRight();
                        LOG.debug("retrieving topology info - topology id: {}, asUser: {}", stormTopologyId, asUser);
                        return client.getTopology(stormTopologyId, asUser);
                    }
                });
        componentRetrieveCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_SIZE_COMPONENT_CACHE)
                .expireAfterWrite(CACHE_DURATION_SECS, TimeUnit.SECONDS)
                .build(new CacheLoader<Pair<Pair<String, String>, String>, Map<String, ?>>() {
                    @Override
                    public Map<String, ?> load(Pair<Pair<String, String>, String> componentIdAndAsUserPair) {
                        String topologyId = componentIdAndAsUserPair.getLeft().getLeft();
                        String componentId = componentIdAndAsUserPair.getLeft().getRight();
                        String asUser = componentIdAndAsUserPair.getRight();
                        LOG.debug("retrieving component info - topology id: {}, component id: {}, asUser: {}",
                                topologyId, componentId, asUser);
                        return client.getComponent(topologyId, componentId, asUser);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyMetric getTopologyMetric(TopologyLayout topology, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (StringUtils.isEmpty(topologyId)) {
            throw new TopologyNotAliveException("Topology not found in Storm Cluster - topology id: " + topology.getId());
        }

        Map<String, ?> responseMap = getTopologyInfo(topologyId, asUser);

        Long uptimeSeconds = ((Number) responseMap.get(TOPOLOGY_JSON_UPTIME_SECS)).longValue();
        String status = (String) responseMap.get(TOPOLOGY_JSON_STATUS);
        Long workerTotal = ((Number) responseMap.get(TOPOLOGY_JSON_WORKERS_TOTAL)).longValue();
        Long executorTotal = ((Number) responseMap.get(TOPOLOGY_JSON_EXECUTORS_TOTAL)).longValue();

        List<Map<String, ?>> topologyStatsList = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_STATS);
        List<Map<String, ?>> spouts = (List<Map<String,?>>) responseMap.get(TOPOLOGY_JSON_SPOUTS);
        List<Map<String, ?>> bolts = (List<Map<String,?>>) responseMap.get(TOPOLOGY_JSON_BOLTS);

        // pick smallest time window
        Map<String, ?> topologyStatsMap = null;
        Long smallestWindow = Long.MAX_VALUE;
        for (Map<String, ?> topoStats : topologyStatsList) {
            String windowStr = (String) topoStats.get(TOPOLOGY_JSON_WINDOW);
            Long window = convertWindowString(windowStr, uptimeSeconds);
            if (smallestWindow > window) {
                smallestWindow = window;
                topologyStatsMap = topoStats;
            }
        }

        // extract metrics from smallest time window
        Long window = smallestWindow;
        Long acked = getLongValueOrDefault(topologyStatsMap, STATS_JSON_ACKED_TUPLES, 0L);
        Long failedRecords = getLongValueOrDefault(topologyStatsMap, STATS_JSON_FAILED_TUPLES, 0L);
        Double completeLatency = getDoubleValueFromStringOrDefault(topologyStatsMap, STATS_JSON_COMPLETE_LATENCY, 0.0d);

        // Storm specific metrics
        Long emittedTotal = getLongValueOrDefault(topologyStatsMap, STATS_JSON_OUTPUT_TUPLES, 0L);
        Long transferred = getLongValueOrDefault(topologyStatsMap, STATS_JSON_TRANSFERRED_TUPLES, 0L);
        Long errorsTotal = getErrorCountFromAllComponents(topologyId, spouts, bolts, asUser);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put(TOPOLOGY_JSON_STATUS, status);
        metrics.put(TOPOLOGY_JSON_WORKERS_TOTAL, workerTotal);
        metrics.put(TOPOLOGY_JSON_EXECUTORS_TOTAL, executorTotal);
        metrics.put(STATS_JSON_UPTIME_SECS, uptimeSeconds);
        metrics.put(STATS_JSON_ACKED_TUPLES, acked);
        metrics.put(STATS_JSON_WINDOW, window);
        metrics.put(STATS_JSON_THROUGHPUT, acked * 1.0 / window);
        metrics.put(STATS_JSON_FAILED_TUPLES, failedRecords);
        metrics.put(STATS_JSON_COMPLETE_LATENCY, completeLatency);
        metrics.put(STATS_JSON_OUTPUT_TUPLES, emittedTotal);
        metrics.put(STATS_JSON_TRANSFERRED_TUPLES, transferred);
        metrics.put(STATS_JSON_TOPOLOGY_ERROR_COUNT, errorsTotal);

        return new TopologyMetric(FRAMEWORK, topology.getName(), metrics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ComponentMetric> getMetricsForTopology(TopologyLayout topology, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (StringUtils.isEmpty(topologyId)) {
            throw new TopologyNotAliveException("Topology not found in Storm Cluster - topology id: " + topology.getId());
        }

        Map<String, ?> responseMap = getTopologyInfo(topologyId, asUser);

        Map<String, ComponentMetric> metricMap = new HashMap<>();
        List<Map<String, ?>> spouts = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_SPOUTS);
        extractMetrics(metricMap, spouts, TOPOLOGY_JSON_SPOUT_ID);

        List<Map<String, ?>> bolts = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_BOLTS);
        extractMetrics(metricMap, bolts, TOPOLOGY_JSON_BOLT_ID);

        return metricMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {
        timeSeriesMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeSeriesQuerier getTimeSeriesQuerier() {
        return timeSeriesMetrics.getTimeSeriesQuerier();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Double> getCompleteLatency(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return timeSeriesMetrics.getCompleteLatency(topology, component, from, to, asUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return timeSeriesMetrics.getkafkaTopicOffsets(topology, component, from, to, asUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeSeriesComponentMetric getTopologyStats(TopologyLayout topology, long from, long to, String asUser) {
        return timeSeriesMetrics.getTopologyStats(topology, from, to, asUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeSeriesComponentMetric getComponentStats(TopologyLayout topology, Component component, long from, long to, String asUser) {
        return timeSeriesMetrics.getComponentStats(topology, component, from, to, asUser);
    }

    private long getErrorCountFromAllComponents(String topologyId, List<Map<String, ?>> spouts, List<Map<String, ?>> bolts, String asUser) {
        LOG.debug("[START] getErrorCountFromAllComponents - topology id: {}, asUser: {}", topologyId, asUser);
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            List<String> componentIds = new ArrayList<>();

            if (spouts != null) {
                for (Map<String, ?> spout : spouts) {
                    componentIds.add((String) spout.get(TOPOLOGY_JSON_SPOUT_ID));
                }
            }

            if (bolts != null) {
                for (Map<String, ?> bolt : bolts) {
                    componentIds.add((String) bolt.get(TOPOLOGY_JSON_BOLT_ID));
                }
            }

            // query to components in parallel
            long errorCount = ParallelStreamUtil.execute(() ->
                            componentIds.parallelStream().mapToLong(componentId -> {
                                Map componentStats = getComponentInfo(topologyId, componentId, asUser);
                                List<?> componentErrors = (List<?>) componentStats.get(TOPOLOGY_JSON_COMPONENT_ERRORS);
                                if (componentErrors != null && !componentErrors.isEmpty()) {
                                    return componentErrors.size();
                                } else {
                                    return 0;
                                }
                            }).sum(), FORK_JOIN_POOL);

            LOG.debug("[END] getErrorCountFromAllComponents - topology id: {}, elapsed: {} ms", topologyId,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return errorCount;
        } finally {
            stopwatch.stop();
        }
    }

    private void extractMetrics(Map<String, ComponentMetric> metricMap, List<Map<String, ?>> components, String topologyJsonID) {
        for (Map<String, ?> component : components) {
            String name = (String) component.get(topologyJsonID);
            String componentId = StormTopologyUtil.extractStreamlineComponentId(name);
            ComponentMetric metric = extractMetric(name, component);
            metricMap.put(componentId, metric);
        }
    }

    private ComponentMetric extractMetric(String componentName, Map<String, ?> componentMap) {
        Map<String, Object> metrics  = new HashMap<>();
        metrics.put(STATS_JSON_EXECUTED_TUPLES, getLongValueOrDefault(componentMap, STATS_JSON_EXECUTED_TUPLES, 0L));
        metrics.put(STATS_JSON_OUTPUT_TUPLES, getLongValueOrDefault(componentMap, STATS_JSON_OUTPUT_TUPLES, 0L));
        metrics.put(STATS_JSON_FAILED_TUPLES, getLongValueOrDefault(componentMap, STATS_JSON_FAILED_TUPLES, 0L));
        metrics.put(STATS_JSON_PROCESS_LATENCY, getDoubleValueFromStringOrDefault(componentMap, STATS_JSON_PROCESS_LATENCY, 0.0d));
        return new ComponentMetric(StormTopologyUtil.extractStreamlineComponentName(componentName), metrics);
    }

    private Long convertWindowString(String windowStr, Long uptime) {
        if (windowStr.equals(":all-time")) {
            return uptime;
        } else {
            return Long.valueOf(windowStr);
        }
    }

    private Long getLongValueOrDefault(Map<String, ?> map, String key, Long defaultValue) {
        if (map.containsKey(key)) {
            Number number = (Number) map.get(key);
            if (number != null) {
                return number.longValue();
            }
        }
        return defaultValue;
    }

    private Double getDoubleValueFromStringOrDefault(Map<String, ?> map, String key,
        Double defaultValue) {
        if (map.containsKey(key)) {
            String valueStr = (String) map.get(key);
            if (valueStr != null) {
                try {
                    return Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    // noop
                }
            }
        }
        return defaultValue;
    }

    private Map<String, ?> getTopologyInfo(String topologyId, String asUser) {
        LOG.debug("[START] getTopologyInfo - topology id: {}, asUser: {}", topologyId, asUser);
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            Map<String, ?> responseMap;
            try {
                responseMap = topologyRetrieveCache.get(new ImmutablePair<>(topologyId, asUser));
            } catch (ExecutionException e) {
                if (e.getCause() != null) {
                    throw new RuntimeException(e.getCause());
                } else {
                    throw new RuntimeException(e);
                }

            } catch (UncheckedExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException(e);
                }
            }

            LOG.debug("[END] getTopologyInfo - topology id: {}, elapsed: {} ms", topologyId,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return responseMap;
        } finally {
            stopwatch.stop();
        }
    }

    private Map<String, ?> getComponentInfo(String topologyId, String componentId, String asUser) {
        // FIXME: we still couldn't handle the case which contains auxiliary part on component name... how to handle?
        LOG.debug("[START] getComponentInfo - topology id: {}, component id: {}, asUser: {}", topologyId, componentId, asUser);
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            Map<String, ?> responseMap;
            try {
                responseMap = componentRetrieveCache.get(new ImmutablePair<>(new ImmutablePair<>(topologyId, componentId), asUser));
            } catch (ExecutionException e) {
                if (e.getCause() != null) {
                    throw new RuntimeException(e.getCause());
                } else {
                    throw new RuntimeException(e);
                }

            } catch (UncheckedExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException(e);
                }
            }

            LOG.debug("[END] getComponentInfo - topology id: {}, component id: {}, elapsed: {} ms", topologyId,
                    componentId, stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return responseMap;
        } finally {
            stopwatch.stop();
        }
    }


    private String buildStormRestApiRootUrl() throws ConfigException {
        // Assuming that a namespace has one mapping of engine
        Service engineService = topologyCatalogHelperService.getFirstOccurenceServiceForNamespace(namespace, engine.getName());
        if (engineService == null) {
            throw new ConfigException("Engine " + engine + " is not associated to the namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }
        com.hortonworks.streamline.streams.cluster.catalog.Component uiServer = topologyCatalogHelperService.getComponent(engineService, COMPONENT_NAME_STORM_UI_SERVER)
                .orElseThrow(() -> new RuntimeException(engine + " doesn't have " + COMPONENT_NAME_STORM_UI_SERVER + " as component"));
        Collection<ComponentProcess> uiServerProcesses = topologyCatalogHelperService.listComponentProcesses(uiServer.getId());
        if (uiServerProcesses.isEmpty()) {
            throw new ConfigException(engine + " doesn't have any process for " + COMPONENT_NAME_STORM_UI_SERVER + " as component");
        }
        ComponentProcess uiServerProcess = uiServerProcesses.iterator().next();
        String uiHost = uiServerProcess.getHost();
        Integer uiPort = uiServerProcess.getPort();
        assertHostAndPort(uiServer.getName(), uiHost, uiPort);
        return "http://" + uiHost + ":" + uiPort + "/api/v1";

    }

    private void assertHostAndPort(String componentName, String host, Integer port) {
        if (host == null || host.isEmpty() || port == null) {
            throw new RuntimeException(componentName + " component doesn't have enough information - host: " + host +
                    " / port: " + port);
        }
    }

}