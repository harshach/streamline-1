package com.hortonworks.streamline.streams.metrics;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import junit.framework.TestCase;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.hortonworks.streamline.streams.metrics.M3MetricsQuerier.M3_SERVICE_CONFIG_KEY_HOST;
import static com.hortonworks.streamline.streams.metrics.M3MetricsQuerier.M3_SERVICE_CONFIG_KEY_PORT;
import static com.hortonworks.streamline.streams.metrics.topology.client.M3RestAPIClient.M3QL_RENDER_ENDPOINT;
import static com.hortonworks.streamline.streams.metrics.topology.client.M3RestAPIClient.TARGET;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.X_UBER_ORIGIN;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.X_UBER_SOURCE;

@RunWith(JMockit.class)
public class M3MetricsQuerierTest extends TestCase {

    private static final String M3_RESPONSE_BODY_ALIASED_BY_TAGS = "/m3-metric-response.json";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18089);
    private M3MetricsQuerier querier;
    private Engine engine;
    private Namespace namespace;
    private Subject subject;


    @Mocked
    private TopologyCatalogHelperService topologyCatalogHelperService;

    @Mocked
    private ServiceConfiguration m3ServiceConfig;

    @Before
    public void setUp() throws Exception {
        querier = new M3MetricsQuerier();

        engine = new Engine();
        engine.setName("PIPER");
        namespace = new Namespace();
        subject = new Subject();
        Map<String, Object> conf = new HashMap<>();

        Service service = new Service();
        service.setId(1L);

        Map<String, String> m3ServiceConfigMap = new HashMap();
        m3ServiceConfigMap.put(M3_SERVICE_CONFIG_KEY_HOST, "localhost");
        m3ServiceConfigMap.put(M3_SERVICE_CONFIG_KEY_PORT, "18089");

        new Expectations() {{
            m3ServiceConfig.getConfigurationMap();
            result = m3ServiceConfigMap;

            topologyCatalogHelperService.getFirstOccurenceServiceForNamespace(namespace, "M3");
            result = service;

            topologyCatalogHelperService.getServiceConfigurationByName(
                    1L, "properties");
            result = m3ServiceConfig;
        }};

        querier.init(engine, namespace, topologyCatalogHelperService, subject, conf);
    }


        @Test
    public void testGetMetricsByTag() throws IOException {
        String target = "fetch env:$dc service:piper name:piper_task_monitoring reason:memory_usage pipeline:$pipeline_id | aliasByTags task";
        // The partially encoded param (+ for space) for the wirerule verify matcher below.
        String partiallyEncodedTargetForMatching = "fetch+env:dca1+service:piper+name:piper_task_monitoring+reason:memory_usage+pipeline:b568289c_3a2a_11e9_bfe1_a0369f72d8ec+|+aliasByTags+task+";
        // The encoded target param that is the param actually sent to endpoint, used in stub url to match stub response.
        String encodedTarget = "fetch+env%3Adca1+service%3Apiper+name%3Apiper_task_monitoring+reason%3Amemory_usage+pipeline%3Ab568289c_3a2a_11e9_bfe1_a0369f72d8ec+%7C+aliasByTags+task+";

        stubMetricUrl(encodedTarget, M3_RESPONSE_BODY_ALIASED_BY_TAGS);

        Map<String, String> metricParams = new HashMap<>();
        metricParams.put("pipeline_id", "b568289c-3a2a-11e9-bfe1-a0369f72d8ec");
        metricParams.put("dc", "dca1");

        long from = 1551243906L * 1000L;
        long to = 1551244506L * 1000L;

        Map<String, Map<Long, Double>> seriesByTag = querier.getMetricsByTag(target, metricParams, from, to);

        Assert.assertNotNull(seriesByTag);
        // verify that we we are correctly returning series by tag
        Map<Long, Double> series = seriesByTag.get("bash");
        Assert.assertNotNull(series);
        // verify null values have been removed
        Assert.assertEquals(series.keySet().size(), 3);
        // verify the entries are in order
        Map.Entry<Long, Double> firstEntry = series.entrySet().iterator().next();
        // verify we are returning converting back to milliseconds
        Assert.assertEquals(firstEntry.getKey(), new Long(1551243976000L));
        Assert.assertEquals(firstEntry.getValue(), new Double(52.0));

        verify(getRequestedFor(urlPathEqualTo(M3QL_RENDER_ENDPOINT))
                .withHeader(X_UBER_ORIGIN, matching(".*"))
                .withHeader(X_UBER_SOURCE, matching(".*"))
                .withHeader("Accept", matching("application/json"))
                .withQueryParam("target", equalTo(partiallyEncodedTargetForMatching))
                .withQueryParam("format", equalTo("json"))
                // verify that we are correctly sending seconds not milliseconds to M3
                .withQueryParam("from", equalTo(Long.toString(from/1000L)))
                .withQueryParam("until", equalTo(Long.toString(to/1000L))));
    }

    private String stubResponseBody(String filename) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(filename)) {
            String body = IOUtils.toString(is, Charset.forName("UTF-8"));
            return body;
        }
    }

    private void stubMetricUrl(String queryTemplate, String bodyFilename) throws IOException {
        String body = stubResponseBody(bodyFilename);

        stubFor(get(urlPathEqualTo(M3QL_RENDER_ENDPOINT))
                .withHeader("Accept", equalTo("application/json"))
                .withQueryParam(TARGET, equalTo(queryTemplate))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

}