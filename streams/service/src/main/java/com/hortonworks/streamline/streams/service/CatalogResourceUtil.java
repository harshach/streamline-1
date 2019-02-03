/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.hortonworks.streamline.streams.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Stopwatch;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.NamespaceServiceClusterMap;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class CatalogResourceUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogResourceUtil.class);

    private CatalogResourceUtil() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class TopologyDashboardResponse {
        private final Topology topology;
        private Map<String, TopologyRuntimeResponse> namespaces;

        public TopologyDashboardResponse(Topology topology) {
            this.topology = topology;
        }

        public void setNamespaces(Map<String, TopologyRuntimeResponse> namespaces) {
            this.namespaces = namespaces;
        }

        public Topology getTopology() {
            return topology;
        }

        public Map<String, TopologyRuntimeResponse> getNamespaces() {
            return namespaces;
        }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class TopologyRuntimeResponse {
        private final String clusterName;
        private final String runtimeTopologyId;
        private TopologyActions.Status status;
        private final TopologyMetrics.TopologyMetric metric;

        public TopologyRuntimeResponse(String clusterName, String runtimeTopologyId, TopologyMetrics.TopologyMetric metric) {
            this.clusterName = clusterName;
            this.runtimeTopologyId = runtimeTopologyId;
            this.metric = metric;
        }

        public String getClusterName() { return clusterName; }

        public String getRuntimeTopologyId() {
            return runtimeTopologyId;
        }

        public TopologyMetrics.TopologyMetric getMetric() {
            return metric;
        }

        public TopologyActions.Status getStatus() {
            return status;
        }

        public void setStatus(TopologyActions.Status status) {
            this.status = status;
        }

    }

    enum TopologyRunningStatus {
        RUNNING, NOT_RUNNING, UNKNOWN
    }

    static TopologyDashboardResponse enrichTopology(Topology topology,
                                                    String asUser,
                                                    EnvironmentService environmentService,
                                                    TopologyActionsService actionsService,
                                                    TopologyMetricsService metricsService,
                                                    StreamCatalogService catalogService) {
        LOG.debug("[START] enrichTopology - topology id: {}", topology.getId());
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            TopologyDashboardResponse detailedResponse;

            String namespaceName = null;
            Namespace namespace = environmentService.getNamespace(topology.getNamespaceId());
            if (namespace != null) {
                namespaceName = namespace.getName();
            }

            try {
                String runtimeTopologyId = actionsService.getRuntimeTopologyId(topology, topology.getNamespaceId(), asUser);
                TopologyMetrics.TopologyMetric topologyMetric = metricsService.getTopologyMetric(topology, asUser);
                detailedResponse = new TopologyDashboardResponse(topology);
                Map<String, TopologyRuntimeResponse> namespaces = new HashMap<>();
                TopologyRuntimeResponse runtimeResponse = new TopologyRuntimeResponse(namespaceName,runtimeTopologyId, topologyMetric);
                runtimeResponse.setStatus(actionsService.topologyStatus(topology, topology.getNamespaceId(), asUser));
                namespaces.put(namespaceName, runtimeResponse);
                detailedResponse.setNamespaces(namespaces);
            } catch (Exception e) {
                LOG.debug("Topology {} is not alive", topology.getId());
                detailedResponse = new TopologyDashboardResponse(topology);
                Map<String, TopologyRuntimeResponse> namespaces = new HashMap<>();
                TopologyRuntimeResponse runtimeResponse = new TopologyRuntimeResponse(namespaceName, "", null);
                runtimeResponse.setStatus(new TopologyActions.Status() {
                    @Override
                    public String getStatus() {
                        return "Unknown";
                    }

                    @Override
                    public Map<String, String> getExtra() {
                        return null;
                    }
                });
                namespaces.put(namespaceName, runtimeResponse);
                detailedResponse.setNamespaces(namespaces);
            }
            LOG.debug("[END] enrichTopology - topology id: {}, elapsed: {} ms", topology.getId(),
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return detailedResponse;
        } finally {
            stopwatch.stop();
        }
    }

    static class NamespaceWithMapping {
        private Namespace namespace;
        private Collection<NamespaceServiceClusterMap> mappings = new ArrayList<>();

        public NamespaceWithMapping(Namespace namespace) {
            this.namespace = namespace;
        }

        public Namespace getNamespace() {
            return namespace;
        }

        public Collection<NamespaceServiceClusterMap> getMappings() {
            return mappings;
        }

        public void setServiceClusterMappings(Collection<NamespaceServiceClusterMap> mappings) {
            this.mappings = mappings;
        }

        public void addServiceClusterMapping(NamespaceServiceClusterMap mapping) {
            mappings.add(mapping);
        }
    }

    static NamespaceWithMapping enrichNamespace(Namespace namespace,
                                                EnvironmentService environmentService) {
        NamespaceWithMapping nm = new NamespaceWithMapping(namespace);
        nm.setServiceClusterMappings(environmentService.listServiceClusterMapping(namespace.getId()));
        return nm;
    }



}
