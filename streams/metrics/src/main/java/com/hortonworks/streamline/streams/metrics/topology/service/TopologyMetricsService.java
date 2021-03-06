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
package com.hortonworks.streamline.streams.metrics.topology.service;

import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Template;
import com.hortonworks.streamline.streams.cluster.container.ContainingNamespaceAwareContainer;
import com.hortonworks.streamline.streams.metrics.TopologyMetricsFactory;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.hortonworks.streamline.streams.catalog.CatalogToLayoutConverter;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class TopologyMetricsService implements ContainingNamespaceAwareContainer {

    final static String processTime = "processLatency";
    private final TopologyMetricsFactory topologyMetricsFactory;
    private final TopologyCatalogHelperService topologyCatalogHelperService;
    private final Subject subject;

    public TopologyMetricsService(TopologyCatalogHelperService topologyCatalogHelperService, Map<String, Object> config, Subject subject) {
        this.topologyCatalogHelperService = topologyCatalogHelperService;
        this.topologyMetricsFactory = new TopologyMetricsFactory(config);
        this.subject = subject;
    }


    /** Delegates to Batch Topology Metric interface **/

    public Map<String, Object> getExecution(Topology topology, Long namespaceId, String executionDate, String asUser) {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getExecution(topology, executionDate, asUser);
    }

    public Map<String, Object> getExecutions(Topology topology, Long namespaceId, Long from, Long to,
                                      Integer page, Integer pageSize, String asUser) {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getExecutions(topology, from, to, page, pageSize, asUser);
    }


    /** Delegates to TopologyTimeSeriesMetric interface **/
    public Map<Long, Double> getTopologyTimeSeriesMetrics(Topology topology, Long namespaceId,
                                                          String metricKeyName, Map<String, String> metricQueryParams,
                                                          long from, long to, String asUser) {

        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getTopologyTimeSeriesMetrics(topology, metricKeyName, metricQueryParams, from, to, asUser);
    }

    public Map<Long, Map<Long, Double>> getComponentTimeSeriesMetrics(Topology topology, Long namespaceId, String metricKeyName,
                                                                      Map<String, String> metricQueryParams, long from,
                                                                      long to, String asUser) throws IOException {

        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getComponentTimeSeriesMetrics(topology, metricKeyName, metricQueryParams, from, to, asUser);
    }

    public Map<Long, Double> getComponentTimeSeriesMetrics(Topology topology, TopologyComponent component, Long namespaceId,
                                                           String metricKeyName, Map<String, String> metricQueryParams,
                                                           long from, long to, String asUser) {

        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getComponentTimeSeriesMetrics(
                topology, component, metricKeyName, metricQueryParams, from, to, asUser);
    }

    public Map<String, TopologyMetrics.ComponentMetric> getComponentMetrics(Topology topology, Long namespaceId,
                                                                            String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getComponentMetrics(topology, asUser);
    }

    public TopologyMetrics.TopologyMetric getTopologyMetric(Topology topology, Long namespaceId,
                                                            String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getTopologyMetric(topology, asUser);
    }

    /** Deprecated Topology Time Series Metric Interfaces **/
    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTopologyStats(Topology topology, Long from, Long to,
                                                                                Long namespaceId, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getTopologyStats(CatalogToLayoutConverter.getTopologyLayout(topology), from, to, asUser);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getComponentStats(Topology topology,
                                                                                 TopologyComponent component, Long from,
                                                                                 Long to, Long namespaceId,
                                                                                 String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getComponentStats(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public Map<String, Map<Long, Double>> getKafkaTopicOffsets(Topology topology, TopologyComponent component, Long from,
                                                               Long to, Long namespaceId, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getkafkaTopicOffsets(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public Map<Long, Double> getCompleteLatency(Topology topology, TopologyComponent component, long from, long to,
                                                Long namespaceId, String asUser) throws Exception {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        return topologyMetrics.getCompleteLatency(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public List<Pair<String, Double>> getTopNAndOtherComponentsLatency(Topology topology, Long namespaceId,
                                                                       String asUser, int nOfTopN) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology, namespaceId);
        Map<String, TopologyMetrics.ComponentMetric> metricsForTopology = topologyMetrics
                .getComponentMetrics(topology, asUser);

        List<Pair<String, Double>> topNAndOther = new ArrayList<>();

        List<ImmutablePair<String, Double>> latencyOrderedComponents = metricsForTopology.entrySet().stream()
                .map((x) -> new ImmutablePair<>(x.getValue().getComponentName(), ((Double) x.getValue().getMetrics().get(processTime))))
                // reversed sort
                .sorted((c1, c2) -> {
                    if (c2 == null) {
                        // assuming c1 is bigger
                        return -1;
                    } else {
                        return c2.getValue().compareTo(c1.getValue());
                    }
                })
                .collect(toList());

        latencyOrderedComponents.stream().limit(nOfTopN).forEachOrdered(topNAndOther::add);
        double sumLatencyOthers = latencyOrderedComponents.stream()
                .skip(nOfTopN).filter((x) -> x.getValue() != null)
                .mapToDouble(Pair::getValue).sum();

        topNAndOther.add(new ImmutablePair<>("Others", sumLatencyOthers));

        return topNAndOther;
    }

    // FIXME T2184621 remove this work around once TopoService and TopoMetrics interfaces are updated.
    public TopologyMetrics getTopologyMetricsInstanceHack(Topology topology, Long namespaceId) {
        return getTopologyMetricsInstance(topology, namespaceId);
    }

    @Override
    public void invalidateInstance(Long namespaceId) { }

    private TopologyMetrics getTopologyMetricsInstance(Topology topology, Long namespaceId) {
        if (namespaceId == null) {
            namespaceId = topology.getNamespaceId();
        }
        Namespace namespace = topologyCatalogHelperService.getNamespace(namespaceId);
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
        }
        Engine engine = topologyCatalogHelperService.getEngine(topology.getEngineId());
        Template template = topologyCatalogHelperService.getTemplate(topology.getTemplateId());
        return topologyMetricsFactory.getTopologyMetrics(engine, namespace, template, topologyCatalogHelperService, subject);
    }
}
