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

import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;

import java.util.Map;

/**
 * Interface which defines methods for querying topology metrics from time-series DB.
 * <p/>
 * Implementation of this interface should convert metric name between Streamline and streaming framework.
 * Converted metric name will be converted once again from TimeSeriesQuerier to perform actual query to time-series DB.
 */
public interface TopologyTimeSeriesMetrics {
    /**
     * Set instance of TimeSeriesQuerier. This method should be called before calling any requests for metrics.
     */
    void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier);

    /**
     * Get instance of TimeSeriesQuerier.
     */
    TimeSeriesQuerier getTimeSeriesQuerier();

    /**
     * Retrieve time series data for Topology.
     *
     * @param topology           topology catalog instance
     * @param metricKeyName      name of metric in engine-metrics.json, used to lookup query.
     * @param metricQueryParams  queries may have params that can be substituted eg:  $topology_id
     * @param from               beginning of the time period: timestamp (in milliseconds)
     * @param to                 end of the time period: timestamp (in milliseconds)
     * @param asUser             username if request needs impersonation to specific user
     * @return Map of data points by timestamp eg.  {timestamp: value} for topology
     */
    Map<Long, Double> getTopologyTimeSeriesMetrics(Topology topology, String metricKeyName,
                                                   Map<String, String> metricQueryParams,
                                                   long from, long to, String asUser);
    /**
     * Retrieve time series data for all of Topology's components.
     *
     * @param topology     topology catalog instance
     * @param metricKeyName      name of metric in engine-metrics.json, used to lookup query.
     * @param metricQueryParams  queries may have params that can be substituted eg:  $topology_id
     * @param from               beginning of the time period: timestamp (in milliseconds)
     * @param to                 end of the time period: timestamp (in milliseconds)
     * @param asUser            username if request needs impersonation to specific user
     * @return Map of Component Id to timeseries data.  eg {componentId: {timestamp: value}}
     */
    Map<Long, Map<Long, Double>> getComponentTimeSeriesMetrics(Topology topology, String metricKeyName,
                                                               Map<String, String> metricQueryParams,
                                                               long from, long to, String asUser);
    /**
     * Retrieve time series data for a particular Topology component.
     *
     * @param topology           topology catalog instance
     @ @param topologyComponent  topologyComponent catalog instance
     * @param metricKeyName      name of metric in engine-metrics.json, used to lookup query
     * @param metricQueryParams  queries may have params that can be substituted eg:  $topology_id
     * @param from               beginning of the time period: timestamp (in milliseconds)
     * @param to                 end of the time period: timestamp (in milliseconds)
     * @param asUser             username if request needs impersonation to specific user
     * @return Map of data points by timestamp eg.  {timestamp: value} for component
     */
    Map<Long, Double> getComponentTimeSeriesMetrics(Topology topology, TopologyComponent topologyComponent,
                                                    String metricKeyName, Map<String, String> metricQueryParams,
                                                    long from, long to, String asUser);


    /**
     * @Deprecated
     *
     * Retrieve "topology stats" on topology.
     * Implementator should aggregate all components' metrics values to make topology stats.
     * The return value is a TimeSeriesComponentMetric which value of componentName is dummy or topology name.
     *
     * @param topology      topology catalog instance
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @param asUser        username if request needs impersonation to specific user
     * @return Map of metric name and Map of data points which are paired to (timestamp, value).
     */
    TimeSeriesComponentMetric getTopologyStats(TopologyLayout topology, long from, long to, String asUser);

    /**
     * @Deprecated
     *
     * Retrieve "complete latency" on source.
     *
     * @param topology  topology catalog instance
     * @param component component layout instance
     * @param from      beginning of the time period: timestamp (in milliseconds)
     * @param to        end of the time period: timestamp (in milliseconds)
     * @param asUser        username if request needs impersonation to specific user
     * @return Map of data points which are paired to (timestamp, value)
     */
    Map<Long, Double> getCompleteLatency(TopologyLayout topology, Component component, long from, long to, String asUser);

    /**
     * @Deprecated
     *
     * Retrieve "kafka topic offsets" on source.
     * <p/>
     * This method retrieves three metrics,<br/>
     * 1) "logsize": sum of partition's available offsets for all partitions<br/>
     * 2) "offset": sum of source's current offsets for all partitions<br/>
     * 3) "lag": sum of lags (available offset - current offset) for all partitions<br/>
     * <p/>
     * That source should be "KAFKA" type and have topic name from configuration.
     *
     * @param topology  topology layout instance
     * @param component component layout instance
     * @param from      beginning of the time period: timestamp (in milliseconds)
     * @param to        end of the time period: timestamp (in milliseconds)
     * @param asUser        username if request needs impersonation to specific user
     * @return Map of metric name and Map of data points which are paired to (timestamp, value).
     */
    Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, Component component, long from, long to, String asUser);

    /**
     * @Deprecated
     *
     * Retrieve "component stats" on component.
     *
     * @param topology      topology catalog instance
     * @param component     component layout instance
     * @param from          beginning of the time period: timestamp (in milliseconds)
     * @param to            end of the time period: timestamp (in milliseconds)
     * @param asUser        username if request needs impersonation to specific user
     * @return Map of metric name and Map of data points which are paired to (timestamp, value).
     */
    TimeSeriesComponentMetric getComponentStats(TopologyLayout topology, Component component, long from, long to, String asUser);


    /**
     * @Deprecated
     *
     * Data structure of Metrics for each component on topology.
     * Fields are extracted from common metrics among various streaming frameworks.
     *
     * Implementors of TopologyTimeSeriesMetrics are encouraged to provide fields' value as many as possible.
     * If field is not available for that streaming framework, implementator can leave it as null or default value.
     */
    class TimeSeriesComponentMetric {
        private final String componentName;
        private final Map<String, Map<Long, Double>> metrics;

        /**
         * Constructor.
         * @param componentName 'component name' for Streamline.
         *                      If component name for streaming framework is different from component name for Streamline,
         *                      implementation of TopologyTimeSeriesMetrics should match the relation.
         * @param metrics          Additional metrics which are framework specific.
         */
        public TimeSeriesComponentMetric(String componentName, Map<String, Map<Long, Double>> metrics) {
            this.componentName = componentName;
            this.metrics = metrics;
        }

        public String getComponentName() {
            return componentName;
        }

        public Map<String, Map<Long, Double>> getMetrics() {
            return metrics;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TimeSeriesComponentMetric)) return false;

            TimeSeriesComponentMetric that = (TimeSeriesComponentMetric) o;

            if (getComponentName() != null ? !getComponentName().equals(that.getComponentName()) : that.getComponentName() != null)
                return false;
            return getMetrics() != null ? getMetrics().equals(that.getMetrics()) : that.getMetrics() == null;
        }

        @Override
        public int hashCode() {
            int result = getComponentName() != null ? getComponentName().hashCode() : 0;
            result = 31 * result + (getMetrics() != null ? getMetrics().hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TimeSeriesComponentMetric{" +
                    "componentName='" + componentName + '\'' +
                    ", misc=" + metrics +
                    '}';
        }
    }
}
