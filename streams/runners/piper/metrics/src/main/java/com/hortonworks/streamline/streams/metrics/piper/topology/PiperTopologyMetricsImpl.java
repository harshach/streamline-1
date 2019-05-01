package com.hortonworks.streamline.streams.metrics.piper.topology;

import com.hortonworks.streamline.common.MetricsUISpec;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.catalog.TopologyRuntimeIdMap;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.register.impl.PiperServiceRegistrar;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import com.hortonworks.streamline.streams.piper.common.PiperRestAPIClient;
import com.hortonworks.streamline.streams.piper.common.PiperUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.*;

import static com.hortonworks.streamline.streams.metrics.TopologyMetricsFactory.METRICS_UI_SPEC;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_ROOT_URL_KEY;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_SERVICE_CONFIG_NAME;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_SERVICE_CONFIG_KEY_HOST;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_SERVICE_CONFIG_KEY_PORT;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RESPONSE_DATA;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RESPONSE_TOTAL_RESULTS;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RESPONSE_PAGE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RESPONSE_PAGE_SIZE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXECUTION_DATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXECUTION_STATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_METRIC_RUNTIME_STATUS;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_METRIC_LATEST_EXECUTION_DATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_METRIC_LATEST_EXECUTION_STATUS;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.RESPONSE_TOTAL_RESULTS;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.RESPONSE_PAGE_SIZE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.RESPONSE_PAGE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXTRAS;

public class PiperTopologyMetricsImpl implements TopologyMetrics {
	private static final Logger LOG = LoggerFactory.getLogger(PiperTopologyMetricsImpl.class);
	private PiperRestAPIClient client;
	private TopologyCatalogHelperService topologyCatalogHelperService;
	private TimeSeriesQuerier timeSeriesQuerier;
	private Namespace namespace;
	private Map<String, Object> configuration;

	// Piper JSON API keys
	private static final String STATE_KEY_SCHEDULE_INTERVAL = "schedule_interval";
    private static final String STATE_KEY_DURATION = "duration";
    private static final String STATE_KEY_AUTOBACKFILLING = "is_auto_backfilling";
    private static final String STATE_KEY_NEXT_EXECUTION_DATE = "next_execution_date";
    private static final String STATE_KEY_TRIGGER_TYPE = "trigger_type";
    private static final String STATE_KEY_TRIGGERED_PIPELINE_RUN_NAME = "triggered_run_name";

    private static final String TASK_GRAPH_KEY_GRAPH= "graph";
    private static final String TASK_GRAPH_KEY_NODES = "nodes";
    private static final String TASK_GRAPH_KEY_STATE = "state";
    private static final String TASK_GRAPH_KEY_START_DATE = "start_date";
    private static final String TASK_GRAPH_KEY_END_DATE = "end_date";
    private static final String TASK_GRAPH_KEY_DURATION = "duration";
    private static final String TASK_GRAPH_KEY_TRY_NUMBER = "try_number";
    private static final String TASK_GRAPH_KEY_RETRIES = "retries";
    private static final String TASK_GRAPH_KEY_POOL = "pool";
    private static final String TASK_GRAPH_KEY_TASK_ID = "task_id";

    private static final String RUNS_KEY_STATE = "state";
    private static final String RUNS_KEY_EXECUTION_DATE = "execution_date";
    private static final String RUNS_KEY_CREATED_AT = "created_at";


    // uWorc JSON Metric keys
    private static final String PIPER_METRIC_FRAMEWORK = "PIPER";

    private static final String PIPER_METRICS_EXECUTIONS = "executions";

    private static final String PIPER_METRIC_COMPONENTS = "components";
    private static final String PIPER_METRIC_COMPONENT_ID = "componentId";
    private static final String PIPER_METRIC_COMPONENT_NAME = "componentName";

    private static final String PIPER_METRIC_CADENCE = "cadence";
    private static final String PIPER_METRIC_DURATION = "duration";
    private static final String PIPER_METRIC_AUTOBACKFILLING = "autobackfilling";
    private static final String PIPER_METRIC_NEXT_EXECUTION_DATE = "nextExecutionDate";
    private static final String PIPER_METRIC_PIPELINE_TYPE = "pipelineType";
    private static final String PIPER_METRIC_TRIGGERED_PIPELINE_RUN_NAME = "triggeredPipelineRunName";

    private static final String PIPER_METRIC_TASK_STATUS = "taskStatus";
    private static final String PIPER_METRIC_TASK_START_DATE = "taskStartDate";
    private static final String PIPER_METRIC_TASK_END_DATE = "taskEndDate";
    private static final String PIPER_METRIC_TASK_DURATION = "taskDuration";
    private static final String PIPER_METRIC_TASK_RETRY_COUNT = "taskRetryCount";
    private static final String PIPER_METRIC_TASK_RETRIES = "taskRetries";
    private static final String PIPER_METRIC_TASK_POOL = "taskPool";

    private static final String PIPER_METRIC_EXECUTION_STATUS = "status";
    private static final String PIPER_METRIC_EXECUTION_DATE = "executionDate";
    private static final String PIPER_METRIC_CREATED_AT = "createdAt";

    private static final String EXTERNAL_TRIGGER = "external_trigger";
    private static final String SCHEDULED_TYPE = "scheduled";
    private static final String TRIGGERED_TYPE = "triggered";



    /**
     * TopologyMetrics Interface Implementation
     *
     *     init
     *     getTopologyMetric
     *     getComponentMetrics
     *     getExecution
     *     getExecutions
     */

    @Override
    public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
                     Map<String, Object> configuration, Subject subject) throws ConfigException {

        this.topologyCatalogHelperService = topologyCatalogHelperService;
        Map<String, Object> piperConf = buildPiperTopologyMetricsConfigMap(namespace, engine);
        String piperAPIRootUrl = (String) piperConf.get(PIPER_ROOT_URL_KEY);
        this.client = new PiperRestAPIClient(piperAPIRootUrl, subject);
        this.namespace = namespace;
        this.configuration = configuration;
    }

    /**
     * Returns Pipeline State (paused, active, next run) and status of latest run.
     *
     * Piper Integration: /api/v1/pipeline/state
     */

    @Override
    public TopologyMetric getTopologyMetric(Topology topology, String asUser) {
        Map<String, Object> metrics = new HashMap<>();

		String runtimeId = getRuntimeTopologyId(topology);
		if (runtimeId != null) {
            Map response = client.getDetailedPipelineState(runtimeId);
            String runtimeStatus = PiperUtil.getRuntimeStatus(response);
            metrics.put(PIPER_METRIC_RUNTIME_STATUS, runtimeStatus);
            metrics.put(PIPER_METRIC_LATEST_EXECUTION_DATE, response.get(STATE_KEY_EXECUTION_DATE));
            metrics.put(PIPER_METRIC_LATEST_EXECUTION_STATUS, response.get(STATE_KEY_EXECUTION_STATE));
            String triggerType = (String) response.get(STATE_KEY_TRIGGER_TYPE);
            metrics.put(PIPER_METRIC_PIPELINE_TYPE, pipelineType(triggerType));

            // FIXME is UI interpreting and humanizing?
            metrics.put(PIPER_METRIC_CADENCE, response.get(STATE_KEY_SCHEDULE_INTERVAL));

            Map<String, Object> extras = (Map<String, Object>) response.get(STATE_KEY_EXTRAS);

            if (extras != null) {
                metrics.put(PIPER_METRIC_DURATION, extras.get(STATE_KEY_DURATION));
                metrics.put(PIPER_METRIC_AUTOBACKFILLING, extras.get(STATE_KEY_AUTOBACKFILLING));
                metrics.put(PIPER_METRIC_NEXT_EXECUTION_DATE, extras.get(STATE_KEY_NEXT_EXECUTION_DATE));
                metrics.put(PIPER_METRIC_TRIGGERED_PIPELINE_RUN_NAME, extras.get(STATE_KEY_TRIGGERED_PIPELINE_RUN_NAME));
            } else {
                // FIXME we were previously returning null for these, so leaving until a contract is agreed on with UI
                metrics.put(PIPER_METRIC_DURATION, null);
                metrics.put(PIPER_METRIC_AUTOBACKFILLING, null);
                metrics.put(PIPER_METRIC_NEXT_EXECUTION_DATE, null);
                metrics.put(PIPER_METRIC_TRIGGERED_PIPELINE_RUN_NAME, null);
            }
        }

		return new TopologyMetric(PIPER_METRIC_FRAMEWORK, topology.getName(), metrics);
	}

    /**
     * Returns latest Pipeline run, with detailed status for each task.
     *
     * Piper Integration:
     *
     *      /api/v1/pipeline/state
     *      /api/v1/pipeline/task_graph
     */
	@Override
	public Map<String, ComponentMetric> getComponentMetrics(Topology topology, String asUser) {
        Map<String, ComponentMetric> metricMap = new HashMap<>();

        String runtimeId = getRuntimeTopologyId(topology);
        if (runtimeId != null) {

            Map response = client.getPipelineState(runtimeId);
            String latestExecutionDate = (String) response.get(STATE_KEY_EXECUTION_DATE);

            if (latestExecutionDate != null) {
                Map<String, Object> execution = getExecution(topology,latestExecutionDate, asUser);
                List<Map<String, Object>> componentList = (List) execution.get(PIPER_METRIC_COMPONENTS);
                for(Map component: componentList) {
                    Long componentId = (Long) component.get(PIPER_METRIC_COMPONENT_ID);
                    String streamlineComponentName = (String) component.get(PIPER_METRIC_COMPONENT_NAME);
                    metricMap.put(componentId.toString(), new ComponentMetric(streamlineComponentName, component));
                }
            }
        }
        return metricMap;
	}

    /**
     * Returns Pipeline run by date (effectively id), with detailed status for each task.
     *
     * Piper Integration:  /api/v1/pipeline/task_graph
     */
    @Override
	public Map<String, Object> getExecution(Topology topology, String executionDate, String asUser) {

		List<Object> componentMetrics = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();

        String runtimeId = getRuntimeTopologyId(topology);
        if (runtimeId != null) {

            Map response = client.getTaskGraph(runtimeId, executionDate);
            Map graph = (Map) response.get(TASK_GRAPH_KEY_GRAPH);
            List tasks = (List) graph.get(TASK_GRAPH_KEY_NODES);
            Map<String, Object> taskMap = toTaskMap(tasks);

            Collection<? extends TopologyComponent> components = getTopologyComponents(topology);
            if (components != null) {
                for (TopologyComponent component : components) {

                    String componentName = component.getName();
                    Map task = (Map) taskMap.getOrDefault(componentName, new HashMap());

                    Map<String, Object> componentMetric = new HashMap<>();

                    componentMetric.put(PIPER_METRIC_COMPONENT_ID, component.getId());
                    componentMetric.put(PIPER_METRIC_COMPONENT_NAME, component.getName());
                    componentMetric.put(PIPER_METRIC_EXECUTION_DATE, executionDate);
                    componentMetric.put(PIPER_METRIC_TASK_STATUS, task.get(TASK_GRAPH_KEY_STATE));
                    componentMetric.put(PIPER_METRIC_TASK_START_DATE, task.get(TASK_GRAPH_KEY_START_DATE));
                    componentMetric.put(PIPER_METRIC_TASK_END_DATE, task.get(TASK_GRAPH_KEY_END_DATE));
                    componentMetric.put(PIPER_METRIC_TASK_DURATION, task.get(TASK_GRAPH_KEY_DURATION));
                    componentMetric.put(PIPER_METRIC_TASK_RETRY_COUNT, task.get(TASK_GRAPH_KEY_TRY_NUMBER));
                    componentMetric.put(PIPER_METRIC_TASK_RETRIES, task.get(TASK_GRAPH_KEY_RETRIES));
                    componentMetric.put(PIPER_METRIC_TASK_POOL, task.get(TASK_GRAPH_KEY_POOL));

                    componentMetrics.add(componentMetric);
                }
            }
        }

		result.put(PIPER_METRIC_COMPONENTS, componentMetrics);
		return result;

	}

    /**
     * Returns Pipeline runs (concise) for date range ordered by most recent.
     *
     * Piper Integration:  /api/v1/pipeline/runs/search
     */

    @Override
	public Map<String, Object> getExecutions(Topology topology, Long from, Long to, Integer page,
                                             Integer pageSize, String asUser) {
		Map<String, Object> result = new HashMap<>();
		ArrayList<Object> topologyMetrics = new ArrayList<Object>();

        String runtimeId = getRuntimeTopologyId(topology);

        if (runtimeId != null) {

            Map response = this.client.getPipelineRuns(runtimeId, toSeconds(from), toSeconds(to), page, pageSize);
            ArrayList<Map<String, Object>> executions = (ArrayList<Map<String, Object>>) response.get(PIPER_RESPONSE_DATA);

            result.put(RESPONSE_TOTAL_RESULTS, response.get(PIPER_RESPONSE_TOTAL_RESULTS));
            result.put(RESPONSE_PAGE, response.get(PIPER_RESPONSE_PAGE));
            result.put(RESPONSE_PAGE_SIZE, response.get(PIPER_RESPONSE_PAGE_SIZE));

            if (executions != null) {
                for (Map execution: executions) {

                    //Abbreviated topology metric
                    Map<String, Object> topologyMetric = new HashMap<>();
                    topologyMetric.put(PIPER_METRIC_EXECUTION_STATUS, execution.get(RUNS_KEY_STATE));
                    topologyMetric.put(PIPER_METRIC_EXECUTION_DATE, execution.get(RUNS_KEY_EXECUTION_DATE));
                    topologyMetric.put(PIPER_METRIC_CREATED_AT, execution.get(RUNS_KEY_CREATED_AT));
                    topologyMetrics.add(topologyMetric);
                }
            }
        }
		result.put(PIPER_METRICS_EXECUTIONS, topologyMetrics);
		return result;
	}

    /**
     * TimeSeriesMetric Interface Implementation
     *
     *      setTimeSeriesQuerier
     *      getTimeSeriesQuerier
     *      getTopologyTimeSeriesMetrics
     *      getComponentTimeSeriesMetrics - all
     *      getComponentTimeSeriesMetrics - single
     */

    @Override
    public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {
        this.timeSeriesQuerier = timeSeriesQuerier;
    }

    @Override
    public TimeSeriesQuerier getTimeSeriesQuerier() {
        return this.timeSeriesQuerier;
    }

    /**
     * Returns M3 timeseries data for pipeline level metrics
     *
     */

    @Override
    public Map<Long, Double> getTopologyTimeSeriesMetrics(Topology topology, String metricKeyName,
                                                          Map<String, String> metricQueryParams,
                                                          long from, long to, String asUser) {

        String metricQueryFormat = getMetricQueryFormat(metricKeyName);

        // merge (overwrite) server params with params from client
        Map<String, String> metricParams = merge(getServerSubstitutionParams(topology), metricQueryParams);

        Map<String, Map<Long, Double>> metricsByTag =
                this.timeSeriesQuerier.getMetricsByTag(metricQueryFormat, metricParams, from, to);

        return metricsByTag.getOrDefault(topology.getName().toLowerCase(), new HashMap());  // M3 downcases all tags
    }

    /**
     * Returns M3 timeseries data for task level metrics
     *
     */

    @Override
    public Map<Long, Map<Long, Double>> getComponentTimeSeriesMetrics(Topology topology, String metricKeyName,
                                                                      Map<String, String> metricQueryParams,
                                                                      long from, long to, String asUser) {
        Map<Long, Map<Long, Double>> results = new HashMap<>();

        String metricQueryFormat = getMetricQueryFormat(metricKeyName);

        // merge (overwrite) server params with params from client
        Map<String, String> metricParams = merge(getServerSubstitutionParams(topology), metricQueryParams);

        Map<String, Map<Long, Double>> metricsByTag =
                this.timeSeriesQuerier.getMetricsByTag(metricQueryFormat, metricParams, from, to);

        Collection<? extends TopologyComponent> components = getTopologyComponents(topology);

        if (components != null) {
            for (TopologyComponent component : components) {
                Map<Long, Double> metrics = metricsByTag.getOrDefault(component.getName().toLowerCase(), new HashMap());  // M3 downcases all tags
                results.put(component.getId(), metrics);
            }
        }
        return results;
    }

    /**
     * Returns M3 timeseries data for an individual task
     *
     */

    @Override
    public Map<Long, Double> getComponentTimeSeriesMetrics(Topology topology, TopologyComponent topologyComponent,
                                                           String metricKeyName, Map<String, String> metricQueryParams,
                                                           long from, long to, String asUser) {
        String metricQueryFormat = getMetricQueryFormat(metricKeyName);

        // merge (overwrite) server params with params from client
        Map<String, String> metricParams = merge(getServerSubstitutionParams(topology), metricQueryParams);

        Map<String, Map<Long, Double>> metricsByTag =
                this.timeSeriesQuerier.getMetricsByTag(metricQueryFormat, metricParams, from, to);

        return metricsByTag.getOrDefault(topologyComponent.getName().toLowerCase(), new HashMap());  // M3 downcases all tags
    }

    /**
     * Deprecated TimeSeriesMetric methods
     *
     *      getTopologyStats
     *      getComponentStats
     *      getCompleteLatency
     *      getkafkaTopicOffsets
     */

    @Override
    public TimeSeriesComponentMetric getTopologyStats(TopologyLayout topology, long from, long to, String asUser) {
        throw new UnsupportedOperationException("getTopologyStats not implemented");
    }

    @Override
    public TimeSeriesComponentMetric getComponentStats(TopologyLayout topology, Component component, long from, long to, String asUser) {
        throw new UnsupportedOperationException("getComponentStats not implemented");
    }

    @Override
    public Map<Long, Double> getCompleteLatency(TopologyLayout topology, Component component, long from, long to, String asUser) {
        throw new UnsupportedOperationException("getCompleteLatency not implemented");
    }

    @Override
    public Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, Component component, long from, long to, String asUser) {
        throw new UnsupportedOperationException("getkafkaTopicOffsets not implemented");
    }

    /**
     * Private Methods
     */

    private String getMetricQueryFormat(String metricKeyName) {
        MetricsUISpec metricsUISpec = (MetricsUISpec) configuration.get(METRICS_UI_SPEC);
        MetricsUISpec.TimeseriesMetricField metricField =
                metricsUISpec.getTimeseries().stream()
                .filter( field -> field.getName().equals(metricKeyName))
                .findFirst()
                .orElse(null);

        return metricField != null ? metricField.getMetricQuery() : null;
    }

	private String getRuntimeTopologyId(Topology topology) {
	    String runtimeId = null;
        TopologyRuntimeIdMap topologyRuntimeIdMap =
                topologyCatalogHelperService.getTopologyRuntimeIdMap(topology.getId(), namespace.getId());
        if (topologyRuntimeIdMap != null) {
            runtimeId = topologyRuntimeIdMap.getApplicationId();
        }
        return runtimeId;
	}

	private Collection<? extends TopologyComponent>  getTopologyComponents(Topology topology) {
        Long currentVersionId = topologyCatalogHelperService.getCurrentVersionId(topology.getId());

        List<com.hortonworks.streamline.common.QueryParam> queryParams =
                WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(topology.getId(), currentVersionId, null);

        return topologyCatalogHelperService.listTopologyTasks(queryParams);
    }

    private Map<String, String> getServerSubstitutionParams(Topology topology) {
        Map<String, String> params = new HashMap<String, String>();
        String runtimeId = getRuntimeTopologyId(topology);
        if (runtimeId != null) {
            params.put("pipeline", runtimeId);
            params.put("pipelineId", runtimeId);
            params.put("applicationId", runtimeId);
        }

        String environment = getPiperEnvironment();
        params.put("dc", environment);
        params.put("env", environment);
        params.put("deployment", environment);
        return params;
    }

    private Map<String, String> merge(Map<String, String> serverParams, Map<String, String> clientParams)  {
        // merge (overwrite) server params with params from client
        for (Map.Entry<String,String> entry : clientParams.entrySet()) {
            serverParams.put(entry.getKey(), (entry.getValue()));
        }
        return serverParams;
    }

    // Piper m3 metrics use an environment tag that seems to map 1:1
    // with the piper yaml config.  The tag muddles environment (prod, staging, dev)
    // and datacenter (sjc1, dca1, phx2).  FIXME unmuddle https://code.uberinternal.com/T2514145
    private String getPiperEnvironment() {
        Map<String, String> configMap = getPiperServiceConfigurationMap(namespace.getId());
        if (configMap == null) {
            throw new IllegalStateException("Piper service config doesn't exist");
        }

        String environment = configMap.get(PiperServiceRegistrar.PARAM_PIPER_API_ENVIRONMENT);
        if (environment == null) {
            throw new IllegalStateException("Piper service config key doesn't exist" + PiperServiceRegistrar.PARAM_PIPER_API_ENVIRONMENT);
        }

        return environment;
    }

    private Map<String, String> getPiperServiceConfigurationMap(Long namespaceId) {
        Map<String, String> piperServiceConfigurationMap = null;

        Namespace namespace = topologyCatalogHelperService.getNamespace(namespaceId);
        if (namespace != null) {
            Service piperService = topologyCatalogHelperService.getFirstOccurenceServiceForNamespace(namespace,
                    PiperServiceRegistrar.SERVICE_NAME);

            if (piperService != null) {
                ServiceConfiguration piperServiceConfig = topologyCatalogHelperService.
                        getServiceConfigurationByName(piperService.getId(), PiperServiceRegistrar.CONF_TYPE_PROPERTIES);

                if (piperServiceConfig != null) {
                    try {
                        piperServiceConfigurationMap = piperServiceConfig.getConfigurationMap();
                    } catch (IOException e) {
                        // Ignored
                    }
                }
            }
        }

        return piperServiceConfigurationMap;
    }


    private Map<String, Object> toTaskMap(List<Map<String, Object>> tasks) {
        Map<String, Object> map = new HashMap<>();
        if (tasks != null) {
            for(Map task: tasks) {
                if (task.get(TASK_GRAPH_KEY_TASK_ID) != null) {
                    map.put((String)task.get(TASK_GRAPH_KEY_TASK_ID), task);
                }
            }
        }
        return map;
    }

	private Map<String, Object> buildPiperTopologyMetricsConfigMap(Namespace namespace, Engine engine) throws ConfigException {
        Map<String, Object> conf = new HashMap<>();

        Service piperService = topologyCatalogHelperService.
                getFirstOccurenceServiceForNamespace(namespace, engine.getName());

        final ServiceConfiguration serviceConfig = topologyCatalogHelperService.getServiceConfigurationByName(
                piperService.getId(), PIPER_SERVICE_CONFIG_NAME);

        if (serviceConfig == null) {
            throw new ConfigException("Serivce Config Not Found " + PIPER_SERVICE_CONFIG_NAME);
        }

        Map<String, String> configMap = null;
        try {
            configMap = serviceConfig.getConfigurationMap();
        } catch (IOException e) {
            throw new ConfigException("Service Config Map could not be loaded " + PIPER_SERVICE_CONFIG_NAME, e);
        }

        if (configMap == null) {
            throw new ConfigException("Serivce Config Map Not Found " + PIPER_SERVICE_CONFIG_NAME);
        }

        String host = configMap.get(PIPER_SERVICE_CONFIG_KEY_HOST);
        String port = configMap.get(PIPER_SERVICE_CONFIG_KEY_PORT);

        String apiRootUrl = PiperUtil.buildPiperRestApiRootUrl(host, port);
        conf.put(PIPER_ROOT_URL_KEY, apiRootUrl);

        return conf;
	}

	private String pipelineType(String trigger_type) {
        String pipelineType = SCHEDULED_TYPE;
        if(EXTERNAL_TRIGGER.equals(trigger_type)) {
            pipelineType = TRIGGERED_TYPE;
        }
        return pipelineType;
    }

    private long toSeconds(long value) {
        return value/1000L;
    };
}
