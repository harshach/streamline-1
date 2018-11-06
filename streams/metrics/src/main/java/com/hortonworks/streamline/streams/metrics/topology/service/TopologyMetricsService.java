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

    public Map<String, TopologyMetrics.ComponentMetric> getTopologyMetrics(Topology topology, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getMetricsForTopology(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);
    }

    public Map<Long, Double> getCompleteLatency(Topology topology, TopologyComponent component, long from, long to, String asUser) throws Exception {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getCompleteLatency(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTopologyStats(Topology topology, Long from, Long to, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getTopologyStats(CatalogToLayoutConverter.getTopologyLayout(topology), from, to, asUser);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getComponentStats(Topology topology, TopologyComponent component, Long from, Long to, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getComponentStats(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public Map<String, Map<Long, Double>> getKafkaTopicOffsets(Topology topology, TopologyComponent component, Long from, Long to, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getkafkaTopicOffsets(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public TopologyMetrics.TopologyMetric getTopologyMetric(Topology topology, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getTopologyMetric(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);
    }

    public List<Pair<String, Double>> getTopNAndOtherComponentsLatency(Topology topology, String asUser, int nOfTopN) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        Map<String, TopologyMetrics.ComponentMetric> metricsForTopology = topologyMetrics
                .getMetricsForTopology(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);

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
    public TopologyMetrics getTopologyMetricsInstanceHack(Topology topology) {
        return getTopologyMetricsInstance(topology);
    }

    @Override
    public void invalidateInstance(Long namespaceId) { }

    private TopologyMetrics getTopologyMetricsInstance(Topology topology) {
        Namespace namespace = topologyCatalogHelperService.getNamespace(topology.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
        }
        Engine engine = topologyCatalogHelperService.getEngine(topology.getEngineId());
        TopologyMetrics topologyMetrics = topologyMetricsFactory.getTopologyMetrics(engine, namespace, topologyCatalogHelperService, subject);
        return topologyMetrics;
    }
}
