package com.hortonworks.streamline.streams.metrics.piper.m3;

import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.metrics.AbstractTimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class M3MetricsWithPiperQuerier extends AbstractTimeSeriesQuerier{

    private static final Logger log = LoggerFactory.getLogger(M3MetricsWithPiperQuerier.class);

    public static final String WILDCARD_ALL_COMPONENTS = "*";

    private Client client;
    private URI renderApiUrl;

    public M3MetricsWithPiperQuerier() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
                     Subject subject, Map<String, Object> conf) throws ConfigException {
        if (conf != null) {
            try {
                client = ClientBuilder.newClient(new ClientConfig());
                renderApiUrl = new URI("http://localhost:14115/m3ql/render");
            } catch (URISyntaxException e) {
                throw new ConfigException(e);
            }
        }
        client = ClientBuilder.newClient(new ClientConfig());
    }

    @Override
    public Map<Long, Double> getTopologyLevelMetrics(String topologyName, String metricName,
                                                     AggregateFunction aggrFunction, long from, long to) {
        return getMetrics(topologyName, WILDCARD_ALL_COMPONENTS, metricName, aggrFunction, from, to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Double> getMetrics(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction,
                                        long from, long to) {

        URI targetUri = composeQueryParameters(topologyName, componentId, metricName, aggrFunction, from, to);

        log.debug("Calling {} for querying metric", targetUri.toString());

        List<Map<String, ?>> responseList = JsonClientUtil.getEntity(client.target(targetUri), List.class);
        if (responseList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ?> metrics = responseList.get(0);
        List<List<Number>> dataPoints = (List<List<Number>>) metrics.get("datapoints");
        return formatDataPointsFromM3ToMap(dataPoints);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<Long, Double>> getRawMetrics(String metricName, String parameters, long from, long to) {
        return Collections.emptyMap();
    }

    private URI composeQueryParameters(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction,
                                       long from, long to) {
        String actualMetricName = "localhost:14115/m3ql/render?target=fetch service:piper name:piper_task_duration pipeline:01950d73_9753_4e83_82eb_910bdd8f8642 env:dca1 timertype:upper | scale 1000 | aliasByTags task";
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder();
        return uriBuilder.uri(renderApiUrl)
                .queryParam("target", actualMetricName)
                .queryParam("format", "json")
                .queryParam("from", String.valueOf((int) (from / 1000)))
                .queryParam("until", String.valueOf((int) (to / 1000)))
                .build();
    }

    private Map<Long, Double> formatDataPointsFromM3ToMap(List<List<Number>> dataPoints) {
        Map<Long, Double> pointsForOutput = new HashMap<>();

        if (dataPoints != null && dataPoints.size() > 0) {
            for (List<Number> dataPoint : dataPoints) {
                // ex. [2940.0, 1465803540] -> 1465803540000, 2940.0
                Number valueNum = dataPoint.get(0);
                Number timestampNum = dataPoint.get(1);
                if (valueNum == null) {
                    continue;
                }
                pointsForOutput.put(timestampNum.longValue() * 1000, valueNum.doubleValue());
            }
        }
        return pointsForOutput;
    }

    public static void main(String[] args) {
        /*System.out.println("hello");
        M3MetricsWithPiperQuerier querier = new M3MetricsWithPiperQuerier();
        String topologyName = "topologyName";
        String componentId = "componentId";
        String metricName = "metricNAme";
        AggregateFunction function = null;
        long from = 0;
        long to = 0;

       try {
            //querier.init(new HashMap<>());
            // disabling to avoid findbugs error until we fix the interfaces.
            //querier.getMetrics(topologyName, componentId, metricName, function, to, from);
        } catch (Exception e) {
            System.out.println(e);
        }*/

    }
}
