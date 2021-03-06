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
package com.hortonworks.streamline.streams.metrics.storm.graphite;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.storm.ambari.AmbariMetricsServiceWithStormQuerier;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphiteWithStormQuerierTest {
    private final String TEST_RENDER_API_PATH = "/render";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18089);
    private GraphiteWithStormQuerier querier;
    private TopologyCatalogHelperService topologyCatalogHelperService;
    private Engine engine;
    private Namespace namespace;
    private Subject subject;

    @Before
    public void setUp() throws Exception {
        querier = new GraphiteWithStormQuerier();

        topologyCatalogHelperService = new TopologyCatalogHelperService(null, null);
        engine = new Engine();
        engine.setName("STORM");
        namespace = new Namespace();
        subject = new Subject();
        Map<String, Object> conf = new HashMap<>();
        conf.put(GraphiteWithStormQuerier.RENDER_API_URL, "http://localhost:18089" + TEST_RENDER_API_PATH);
        conf.put(GraphiteWithStormQuerier.METRIC_NAME_PREFIX, "storm");
        conf.put(GraphiteWithStormQuerier.USE_FQDN, "false");

        querier.init(engine, namespace, topologyCatalogHelperService, subject,conf);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void getMetrics() throws Exception {
        stubMetricUrl();

        String topologyName = "testTopology";
        String componentId = "testComponent";
        String metricName = "__test.metric.name";
        TimeSeriesQuerier.AggregateFunction aggrFunction = TimeSeriesQuerier.AggregateFunction.SUM;
        long from = 1234000L;
        long to = 5678000L;

        Map<Long, Double> metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_RENDER_API_PATH))
                .withQueryParam("target", equalTo("sumSeries(storm.testTopology.testComponent.*.*.*.*.*.*.__test.metric.name)"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("from", equalTo("1234"))
                .withQueryParam("until", equalTo("5678")));
    }

    @Test
    public void getMetricsWithStreamAggregation() throws Exception {
        stubMetricUrl();

        String topologyName = "testTopology";
        String componentId = "testComponent";
        // this is one of metric which needs stream aggregation
        String metricName = "__complete-latency";
        TimeSeriesQuerier.AggregateFunction aggrFunction = TimeSeriesQuerier.AggregateFunction.AVG;
        long from = 1234000L;
        long to = 5678000L;

        Map<Long, Double> metrics = querier.getMetrics(topologyName, componentId, metricName, aggrFunction, from, to);
        assertResult(metrics);

        verify(getRequestedFor(urlPathEqualTo(TEST_RENDER_API_PATH))
                .withQueryParam("target", equalTo("averageSeries(storm.testTopology.testComponent.*.*.*.*.*.*.__complete-latency.*)"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("from", equalTo("1234"))
                .withQueryParam("until", equalTo("5678")));
    }

    @Test
    public void getRawMetrics() throws Exception {
        stubMetricUrl();

        String metricName = "metric";
        String parameters = "";
        long from = 1234000L;
        long to = 5678000L;

        Map<String, Map<Long, Double>> metrics = querier.getRawMetrics(metricName, parameters, from, to);
        assertResult(metrics.get("metric"));

        verify(getRequestedFor(urlPathEqualTo(TEST_RENDER_API_PATH))
                .withQueryParam("target", equalTo("metric"))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("from", equalTo("1234"))
                .withQueryParam("until", equalTo("5678")));
    }

    private void stubMetricUrl() {
        stubFor(get(urlPathEqualTo(TEST_RENDER_API_PATH))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"target\": \"metric\", \"datapoints\": [[456.789, 2345], [890.123, 3456]]}]")));
    }

    private void assertResult(Map<Long, Double> metrics) {
        assertTrue(metrics.containsKey(2345000L));
        assertEquals(456.789, metrics.get(2345000L), 0.00001);
        assertEquals(890.123, metrics.get(3456000L), 0.00001);
    }

}