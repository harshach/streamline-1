package com.hortonworks.streamline.streams.metrics;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.metrics.topology.client.M3RestAPIClient;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.URISyntaxException;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3MetricsQuerier extends AbstractTimeSeriesQuerier {

    private static final Logger LOG = LoggerFactory.getLogger(M3MetricsQuerier.class);

    private static final String M3_SERVICE_NAME = "M3";
    private static final String M3_SERVICE_CONFIG_NAME = "properties";
    private static final String M3_SERVICE_CONFIG_KEY_HOST = "m3.service.host";
    private static final String M3_SERVICE_CONFIG_KEY_PORT = "m3.service.port";
    private static final String TARGET_KEY = "target";
    private static final String DATAPOINTS_KEY = "datapoints";
    private static final String M3_ROOT_URL_KEY = "API_ROOT_URL";

    private static final String DASH = "-";
    private static final String UNDERSCORE = "_";
    private static final String UUID_REGEX = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

    private M3RestAPIClient client;
    private TopologyCatalogHelperService topologyCatalogHelperService;

    public M3MetricsQuerier() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
                     Subject subject, Map<String, Object> conf) throws ConfigException {

        this.topologyCatalogHelperService = topologyCatalogHelperService;

        try {
            Map<String, String> m3Conf = buildM3TimeSeriesQuerierConfigMap(namespace, engine);

            client = new M3RestAPIClient(m3Conf.get(M3_ROOT_URL_KEY), subject);
        } catch (URISyntaxException e) {
            throw new ConfigException(e);
        }
    }

    // FIXME new interface
    public Map<String, Object> getMetricsByTag(String metricQueryTemplate, Map<String, String> metricParams,
                                               long from, long to, String asUser) {

        // M3 discourages UUIDs, masks to prevent M3 from dropping
        maskUUIDs(metricParams);

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

    public static String maskUUID(String uuid) {
        return uuid.replaceAll(DASH, UNDERSCORE);
    }

    public static void maskUUIDs(Map<String,String> params) {
        Matcher matcher = UUID_PATTERN.matcher("");
        for (Map.Entry<String,String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                matcher.reset(value);
                if (matcher.matches()) {
                    params.put(entry.getKey(), maskUUID(value));
                }
            }
        }
    }

    private long toSeconds(long value) {
        return value/1000L;
    };

    private static String appendSpace(String s) {
        return s + " ";
    }

    private Map<String, String> buildM3TimeSeriesQuerierConfigMap(Namespace namespace, Engine engine)
        throws ConfigException {

        Map<String, String> conf = new HashMap<>();

        Service m3Service = topologyCatalogHelperService.
                getFirstOccurenceServiceForNamespace(namespace, M3_SERVICE_NAME);

        if (m3Service == null) {
            throw new ConfigException("Service  Not Found " + M3_SERVICE_NAME);
        }

        final ServiceConfiguration serviceConfig = topologyCatalogHelperService.getServiceConfigurationByName(
                m3Service.getId(), M3_SERVICE_CONFIG_NAME);

        if (serviceConfig == null) {
            throw new ConfigException("Service Config Not Found " + M3_SERVICE_CONFIG_NAME);
        }

        Map<String, String> configMap;
        try {
            configMap = serviceConfig.getConfigurationMap();
        } catch (IOException e) {
            throw new ConfigException("Service Config Map could not be loaded " + M3_SERVICE_CONFIG_NAME, e);
        }

        if (configMap == null) {
            throw new ConfigException("Service Config Map Not Found " + M3_SERVICE_CONFIG_NAME);
        }

        String host = configMap.get(M3_SERVICE_CONFIG_KEY_HOST);
        String port = configMap.get(M3_SERVICE_CONFIG_KEY_PORT);

        String apiRootUrl = "http://" + host + ":" + port;
        conf.put(M3_ROOT_URL_KEY, apiRootUrl);

        return conf;
    }
}