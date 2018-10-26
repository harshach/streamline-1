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
package com.hortonworks.streamline.streams.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.annotations.VisibleForTesting;
import com.hortonworks.streamline.common.util.DoubleUtils;
import com.hortonworks.streamline.streams.metrics.storm.topology.StormMappedMetric;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Note: Given that UI of view mode is tied to Apache Storm, using the term of Storm.
 */
public class ComponentMetricSummary {
    private static final String EMITTED_COUNT = "emitted";
    private static final String ACKED_COUNT = "acked";
    private static final String COMPLETE_LATENCY = "completeLatency";
    private static final String PROCESS_LATENCY = "processLatency";
    private static final String EXECUTE_LATENCY = "executeLatency";
    private static final String INPUT_RECORDS = "inputRecords";
    private static final String FAILED_RECORDS = "failedRecords";

    private final Map<String, Object> metrics;
    private final Map<String, Object> prevMetrics;

    public ComponentMetricSummary(Map<String, Object> metrics, Map<String, Object> prevMetrics) {
        this.metrics = metrics;
        this.prevMetrics = prevMetrics;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public Map<String, Object> getPrevMetrics() { return prevMetrics; }

    public static ComponentMetricSummary convertStreamingComponentMetric(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                               TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevMetrics) {

        Map<String, Object> aggMetrics = aggregateStreamingMetrics(metrics);
        Map<String, Object> aggPrevMetrics = aggregateStreamingMetrics(prevMetrics);

        return new ComponentMetricSummary(aggMetrics, aggPrevMetrics);
    }

    public static Map<String, Object> aggregateStreamingMetrics(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
        Map<String, Object> aggMetrics = new HashMap<>();
        if (metrics == null) return aggMetrics;
        Map<String, Map<Long, Double>> componentMetrics = metrics.getMetrics();
        for (Map.Entry<String, Map<Long, Double>> entry : metrics.getMetrics().entrySet()) {
            if (entry.getKey().equals(StormMappedMetric.inputRecords.name()) || entry.getKey().equals(StormMappedMetric.failedRecords.name())
                    || entry.getKey().equals(StormMappedMetric.outputRecords.name()) || entry.getKey().equals(StormMappedMetric.ackedRecords.name())) {
                aggMetrics.put(entry.getKey(), (entry.getValue() != null ? entry.getValue().values().stream().mapToLong(Double::longValue).sum() : 0.0d));
            }
        }

        // Aggregate Process Latency
        aggMetrics.put(PROCESS_LATENCY, calculateWeightedAverage(componentMetrics.getOrDefault(StormMappedMetric.processedTime.name(), Collections.emptyMap()),
                componentMetrics.getOrDefault(StormMappedMetric.inputRecords.name(), Collections.emptyMap())));

        //Aggregate Execute Latency
        aggMetrics.put(EXECUTE_LATENCY, calculateWeightedAverage(componentMetrics.getOrDefault(StormMappedMetric.executeTime.name(), Collections.emptyMap()),
                componentMetrics.getOrDefault(StormMappedMetric.inputRecords.name(), Collections.emptyMap())));

        // Aggregate Complete Latency
        aggMetrics.put(COMPLETE_LATENCY, calculateWeightedAverage(componentMetrics.getOrDefault(StormMappedMetric.completeLatency.name(),Collections.emptyMap()),
                componentMetrics.getOrDefault(ACKED_COUNT,Collections.emptyMap())));

        return aggMetrics;
    }



    @VisibleForTesting
    static double calculateWeightedAverage(Map<Long, Double> keyMetrics, Map<Long, Double> weightMetrics) {
        Map<Long, Double> filteredKeyMetrics = keyMetrics.entrySet().stream()
                .filter(entry -> entry.getValue() != null && DoubleUtils.notEqualsToZero(entry.getValue()))
                .collect(toMap(e -> e.getKey(), e -> e.getValue()));

        Map<Long, Double> filteredWeightMetrics = weightMetrics.entrySet().stream()
                .filter(entry -> filteredKeyMetrics.containsKey(entry.getKey()))
                .collect(toMap(e -> e.getKey(), e -> e.getValue()));

        Double sumInputRecords = filteredWeightMetrics.values().stream().mapToDouble(d -> d).sum();

        if (DoubleUtils.equalsToZero(sumInputRecords)) {
            // total weight is zero
            return 0.0d;
        }

        return filteredKeyMetrics.entrySet().stream()
                .map(entry -> {
                    Long timestamp = entry.getKey();
                    double weight = filteredWeightMetrics.getOrDefault(timestamp, 0.0) / sumInputRecords;
                    return entry.getValue() * weight;
                }).mapToDouble(d -> d).sum();
    }
}
