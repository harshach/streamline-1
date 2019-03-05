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
package com.hortonworks.streamline.streams.metrics.topology;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Map;

/**
 * Interface that shows metrics for Streamline topology.
 * <p/>
 * Each underlying streaming framework should provide implementations to integrate metrics of framework into Streamline.
 * <p/>
 * Note that this interface also extends TopologyTimeSeriesMetrics, which is for querying topology metrics from time-series DB.
 */
public interface TopologyMetrics extends TopologyTimeSeriesMetrics {
    /**
     * Initialize method. Any one time initialization is done here.
     *
     * @throws ConfigException throw when instance can't be initialized with this configuration (misconfigured).
     */
    void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
              Map<String, Object> configuration, Subject subject) throws ConfigException;

    /**
     * Retrieves topology metric for Streamline topology/
     *
     * @param topology topology catalog instance. Implementations should find actual runtime topology with provided topology.
     * @param asUser   username if request needs impersonation to specific user
     * @return TopologyMetrics
     */
    TopologyMetric getTopologyMetric(Topology topology, String asUser) throws IOException;

    /**
     * Retrieves metrics data for Topology components.
     *
     * @param topology topology catalog instance. Implementations should find actual runtime topology with provided topology.
     * @param asUser   username if request needs impersonation to specific user
     * @return pair of (component id, ComponentMetric instance).
     * Implementations should ensure that component name is same to UI name of component
     * so that it can be matched to Streamline topology.
     */
    Map<String, ComponentMetric> getComponentMetrics(Topology topology, String asUser);


    /**
     * Retrieve execution status by date for a Batch Topology.
     *
     * @param topology      topology catalog instance.
     * @param executionDate executionDate is a unique identifier for the execution
     * @param asUser        username if request needs impersonation to specific user
     * @return FIMXE
     * Batch specific, Streaming engines may choose not to implement.
     */
    Map<String, Object> getExecution(Topology topology, String executionDate, String asUser);

    /**
     * Retrieve list of executions for Batch Topologies, supports Pagination.
     *
     * @param topology      topology catalog instance.
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @param page          page number
     * @param pageSize      results per page
     * @param asUser        username if request needs impersonation to specific user
     * @return FIXME
     * Batch specific, Streaming engines may choose not to implement.
     */
    Map<String, Object> getExecutions(Topology topology, Long from, Long to, Integer page, Integer pageSize, String asUser);


    /**
     * Data structure of Metrics for each component on topology.
     * Fields are extracted from common metrics among various streaming frameworks.
     *
     * Implementors of TopologyMetrics are encouraged to provide fields' value as many as possible.
     * If field is not available for that streaming framework, implementator can leave it as null or default value.
     */
    class TopologyMetric {
        private final String framework;
        private final String topologyName;
        private final Map<String, Object> metrics;

        /**
         * Constructor.
         * @param framework        Which streaming framework runs this topology. (e.g. Storm, Spark Streaming, etc.)
         * @param topologyName  'topology name' for Streams.
         *                      If topology name for streaming framework is different from topology name for Streams,
         *                      implementation of TopologyMetrics should match the relation.
         * @param metrics       Metrics for the application
         */
        public TopologyMetric(String framework, String topologyName, Map<String, Object> metrics) {
            this.framework = framework;
            this.topologyName = topologyName;
            this.metrics = metrics;
        }

        public String getFramework() { return framework; }

        public String getTopologyName() { return topologyName; }

        public Map<String, Object> getMetrics() {
            return metrics;
        }
    }

    /**
     * Data structure of Metrics for each component on topology.
     * Fields are extracted from common metrics among various streaming frameworks.
     *
     * Implementors of TopologyMetrics are encouraged to provide fields' value as many as possible.
     * If field is not available for that streaming framework, implementator can leave it as null or default value.
     */
    class ComponentMetric {
        private final String componentName;
        private final Map<String, Object> metrics;

        /**
         * Constructor.
         *
         * @param componentName 'component name' for Streamline.
         *                      If component name for streaming framework is different from component name for Streamline,
         *                      implementation of TopologyMetrics should match the relation.
         * @param metrics component level metrics
         */
        public ComponentMetric(String componentName, Map<String, Object> metrics) {
            this.componentName = componentName;
            this.metrics = metrics;
        }

        public String getComponentName() {
            return componentName;
        }

        public Map<String, Object> getMetrics() {
            return metrics;
        }

    }
}
