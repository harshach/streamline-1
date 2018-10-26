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

import com.hortonworks.streamline.common.util.DoubleUtils;
import com.hortonworks.streamline.streams.metrics.storm.topology.StormMappedMetric;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ComponentMetricSummaryTest {

    @Test
    public void testCalculateWeightedAverage() {
        Map<Long, Double> keyMetrics = new HashMap<>();
        keyMetrics.put(1L, 10.0);
        keyMetrics.put(2L, 20.0);

        Map<Long, Double> weightMetrics = new HashMap<>();
        weightMetrics.put(1L, 10.0);
        weightMetrics.put(2L, 5.0);

        double actual = ComponentMetricSummary.calculateWeightedAverage(keyMetrics, weightMetrics);

        // 10.0 * 10.0 / (10.0 + 5.0) + 20.0 * 5.0 / (10.0 + 5.0)
        double expected = 13.333333333d;

        Assert.assertEquals(expected, actual, DoubleUtils.EPSILON);
    }

    @Test
    public void testCalculateWeightedAverageLacksWeightInformation() {
        Map<Long, Double> keyMetrics = new HashMap<>();
        keyMetrics.put(1L, 10.0);
        keyMetrics.put(2L, 20.0);

        // no weight for both 1L and 2L
        double actual = ComponentMetricSummary.calculateWeightedAverage(keyMetrics, Collections.emptyMap());
        Assert.assertEquals(0.0, actual, DoubleUtils.EPSILON);

        Map<Long, Double> weightMetrics = new HashMap<>();
        // no weight for 1L
        weightMetrics.put(2L, 5.0);

        actual = ComponentMetricSummary.calculateWeightedAverage(keyMetrics, weightMetrics);

        // only weight and value for 2L is considered
        Assert.assertEquals(20.0, actual, DoubleUtils.EPSILON);
    }

    private TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTestMetric() {
        Map<String, Map<Long, Double>> metrics = new HashMap<>();
        Map<Long, Double> inputRecords = new HashMap<>();
        inputRecords.put(1L, 10.0d);
        inputRecords.put(2L, 20.0d);
        metrics.put(StormMappedMetric.inputRecords.name(), inputRecords);

        Map<Long, Double> outputRecords = new HashMap<>();
        outputRecords.put(1L, 20.0d);
        outputRecords.put(2L, 40.0d);
        metrics.put(StormMappedMetric.outputRecords.name(), outputRecords);

        Map<Long, Double> failedRecords = new HashMap<>();
        failedRecords.put(1L, 1.0d);
        failedRecords.put(2L, 2.0d);
        metrics.put(StormMappedMetric.failedRecords.name(), failedRecords);

        Map<Long, Double> processedTime = new HashMap<>();
        processedTime.put(1L, 1.234d);
        processedTime.put(2L, 4.567d);
        metrics.put(StormMappedMetric.processedTime.name(), processedTime);

        Map<Long, Double> recordsInWaitQueue = new HashMap<>();
        recordsInWaitQueue.put(1L, 1d);
        recordsInWaitQueue.put(2L, 2d);
        metrics.put(StormMappedMetric.recordsInWaitQueue.name(), recordsInWaitQueue);


        Map<Long, Double> executeTime = new HashMap<>();
        executeTime.put(1L, 0.123d);
        executeTime.put(2L, 0.456d);

        Map<Long, Double> completeLatency = new HashMap<>();
        completeLatency.put(1L, 123.456d);
        completeLatency.put(2L, 456.789d);

        Map<Long, Double> ackedRecords = new HashMap<>();
        ackedRecords.put(1L, 15.0d);
        ackedRecords.put(2L, 25.0d);

        metrics.put(StormMappedMetric.executeTime.name(), executeTime);
        metrics.put(StormMappedMetric.completeLatency.name(), completeLatency);
        metrics.put("ackedRecords", ackedRecords);

        return new TopologyTimeSeriesMetrics.TimeSeriesComponentMetric("testComponent", metrics);
    }

}