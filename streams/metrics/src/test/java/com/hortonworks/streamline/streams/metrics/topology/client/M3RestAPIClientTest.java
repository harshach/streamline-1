package com.hortonworks.streamline.streams.metrics.topology.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.hortonworks.streamline.streams.metrics.topology.client.M3RestAPIClient.M3QL_RENDER_ENDPOINT;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.X_UBER_ORIGIN;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.X_UBER_SOURCE;

public class M3RestAPIClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(18089);

    
    @Test
    public void getMetrics() throws Exception {
        stubMetricUrl();

        M3RestAPIClient client = new M3RestAPIClient("http://localhost:18089", null);

        long from = 1551243906L;
        long until = 1551244506L;

        String target = "fetch env:dca1 service:piper name:piper_task_monitoring reason:memory_usage pipeline:b568289c_3a2a_11e9_bfe1_a0369f72d8ec | aliasByTags task";
        String encodedTarget = "fetch+env:dca1+service:piper+name:piper_task_monitoring+reason:memory_usage+pipeline:b568289c_3a2a_11e9_bfe1_a0369f72d8ec+|+aliasByTags+task";

        // Note - The client sends url-encoded params - it uses % encoding except for spaces which are encoded as +.
        //        The WireMock matcher decodes % for the comparison but does not handle the '+'.  So the test string
        //        we use here is not the actual value sent to server.  The actual string sent to the M3 server is below:
        //
        // String encodedTarget = "fetch+env%3Adca1+service%3Apiper+name%3Apiper_task_monitoring+reason%3Amemory_usage+pipeline%3Ab568289c_3a2a_11e9_bfe1_a0369f72d8ec+%7C+aliasByTags+task";


        List<Map> results = client.getMetrics(target, from, until);

        Assert.assertNotNull(results);
        Assert.assertEquals(results.size(), 2);
        Map firstSeries = results.get(1);
        Assert.assertNotNull(firstSeries.get("target"));
        Assert.assertNotNull(firstSeries.get("datapoints"));

        verify(getRequestedFor(urlPathEqualTo(M3QL_RENDER_ENDPOINT))
                .withHeader(X_UBER_ORIGIN, matching(".*"))
                .withHeader(X_UBER_SOURCE, matching(".*"))
                .withHeader("Accept", matching("application/json"))
                .withQueryParam("target", equalTo(encodedTarget))
                .withQueryParam("format", equalTo("json"))
                .withQueryParam("from", equalTo(Long.toString(from)))
                .withQueryParam("until", equalTo(Long.toString(until))));
    }

    /*
        Stub response created with curl like (may need to adjust from, until):
            curl -v -H "X-Uber-Origin: metric_alias" -H "X-Uber-Source: pcullen@mquery01-dca1" "http://localhost:14115/m3ql/render?target=fetch+service%3Apiper+name%3Apiper_task_duration+pipeline%3A1f592d91_5745_4bc9_89f4_bb572b4293eb+env%3Adca1+timertype%3Aupper+%7C+scale+1000+%7C+aliasByTags+task&format=json&from=1551216507&until=1551227307"
    */

    private String stubResponseBody() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/m3-metric-response.json")) {
            String body = IOUtils.toString(is, Charset.forName("UTF-8"));
            return body;
        }
    }

    private void stubMetricUrl() throws IOException {
        String body = stubResponseBody();

        stubFor(get(urlPathEqualTo(M3QL_RENDER_ENDPOINT))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

}