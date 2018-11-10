package com.hortonworks.streamline.streams.metrics.piper.m3;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.metrics.AbstractTimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.client.M3RestAPIClient;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.net.URISyntaxException;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class M3MetricsWithPiperQuerier extends AbstractTimeSeriesQuerier{

    private static final Logger LOG = LoggerFactory.getLogger(M3MetricsWithPiperQuerier.class);

    private static final String TARGET_KEY = "target";
    private static final String DATAPOINTS_KEY = "datapoints";


    private M3RestAPIClient client;

    public M3MetricsWithPiperQuerier() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
                     Subject subject, Map<String, Object> conf) throws ConfigException {

        try {
            client = new M3RestAPIClient("http://localhost:14115", null);
        } catch (URISyntaxException e) {
            throw new ConfigException(e);
        }
    }

    // FIXME new interface
    public Map<String, Object> getMetricsByTag(String metricQueryTemplate, Map<String, String> metricParams,
                                               long from, long to, String asUser) {

        // Substitute params into query
        String metricQuery = substitute(metricQueryTemplate, metricParams);

        // Fetch M3 metrics
        List<Map> m3QueryResults = client.getMetrics(metricQuery, toSeconds(from), toSeconds(to), asUser);

        Map<String, Object> results = new HashMap<>();
        for (Map target: m3QueryResults ) {
            String targetTag = (String) target.get(TARGET_KEY);
            List<List<Number>> dataPoints = (List<List<Number>>) target.get(DATAPOINTS_KEY);
            Map<Long, Double> formatedDataPoints = formatDataPointsFromM3ToMap(dataPoints);
            results.put(targetTag, formatedDataPoints);
        }

        return results;
    }

    @Override
    public Map<Long, Double> getTopologyLevelMetrics(String topologyName, String metricName,
                                                     AggregateFunction aggrFunction, long from, long to) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Double> getMetrics(String topologyName, String componentId, String metricName, AggregateFunction aggrFunction,
                                        long from, long to) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<Long, Double>> getRawMetrics(String metricName, String parameters, long from, long to) {
        return null;
    }

    private Map<Long, Double> formatDataPointsFromM3ToMap(List<List<Number>> dataPoints) {
        Map<Long, Double> pointsForOutput = new TreeMap<>();

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

    private String substitute(String queryFormat, Map<String,String> params)  {
        StrSubstitutor substitutor = new StrSubstitutor(params, "$", " ");

        for (Map.Entry<String,String> entry : params.entrySet()) {
            params.put(entry.getKey(), appendSpace(entry.getValue()));
        }

        String query = substitutor.replace(queryFormat);

        if (query.contains("$")) {
            throw new IllegalStateException(String.format("Query template has unsubstituted params: %s ", query));
        }
        return query;
    }

    private long toSeconds(long value) {
        return value/1000L;
    };

    private static String appendSpace(String s) {
        return s + " ";
    }

}
