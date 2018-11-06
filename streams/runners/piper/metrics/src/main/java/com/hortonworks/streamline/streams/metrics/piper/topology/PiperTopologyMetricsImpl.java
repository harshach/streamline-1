package com.hortonworks.streamline.streams.metrics.piper.topology;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import com.hortonworks.streamline.streams.piper.common.PiperRestAPIClient;
import org.apache.avro.generic.GenericData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.util.*;


public class PiperTopologyMetricsImpl implements TopologyMetrics {
	private static final Logger LOG = LoggerFactory.getLogger(PiperTopologyMetricsImpl.class);
	private PiperRestAPIClient client;

	@Override
	public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {

	}

	@Override
	public TimeSeriesComponentMetric getTopologyStats(TopologyLayout topology, long from, long to, String asUser) {
		return null;
	}

	@Override
	public Map<Long, Double> getCompleteLatency(TopologyLayout topology, Component component, long from, long to, String asUser) {
		return null;
	}

	@Override
	public Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, Component component, long from, long to, String asUser) {
		return null;
	}

	@Override
	public TimeSeriesComponentMetric getComponentStats(TopologyLayout topology, Component component, long from, long to, String asUser) {
		return null;
	}

	@Override
	public TimeSeriesQuerier getTimeSeriesQuerier() {
		return null;
	}

	@Override
	public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService,
					 Subject subject, Map<String, Object> conf) throws ConfigException {
		String piperAPIRootUrl = (String)conf.get("PIPER_API_ROOT_URL_KEY");
		this.client = new PiperRestAPIClient(piperAPIRootUrl, null);
	}

	@Override
	// FIXME needs applicationId, catalogService or impl needs to init with catalogService (StormImpl asks the storm server)
	// FIXME we could use this for getExecutions if it accepted params
	// FIXME Workaround pending T2184545
	public TopologyMetric getTopologyMetric(TopologyLayout topology, String asUser) {
		String applicationId = "f7a8eef8-d1a2-11e8-9217-8c859066bcf7";
		Map response = this.client.getPipelineState(applicationId, null);

		/*  FIXME transform.
			FIXME needs to be implemented for project listing

			"metricKeyName": "status"
			"comment": "Pipeline is active and not paused."

			"metricKeyName": "latest_execution_date",
			"comment": "Identifier for this pipeline run (execution date)."


			"metricKeyName": "latest_execution_status",
			"valueFormat": "string",
			"defaultValue": ""


			"metricKeyName": "duration",
			"valueFormat": "time",

			"metricKeyName": "auto_backfilling",
			"valueFormat": "boolean",

			"metricKeyName": "next_execution_date",
			"valueFormat": "datetime",


			"metricKeyName": "pipeline_type",
			"valueFormat": "string",
			"defaultValue": "",
			"comment": "scheduled or triggered"

			"metricKeyName": "cadence",
			"valueFormat": "string",

			"metricKeyName": "triggered_pipeline_runname",
			"valueFormat": "string",
			"comment": "only present for triggered pipelines, run name"
		*/

		return new TopologyMetric("PIPER", topology.getName(), response);
	}

	@Override
	// FIXME T2184613 - needs applicationId, catalogService or impl needs to init with catalogService (StormImpl asks the storm server)
	// FIXME we could use this for getExecution if it accepted params
	public Map<String, ComponentMetric> getMetricsForTopology(TopologyLayout topology, String asUser) {
		// Stubbed
		String streamlineComponentName = "COMPONENT_ID";
		Map<String, Object> map = new HashMap<>();
		Map<String, ComponentMetric> metricMap = new HashMap<>();
		ComponentMetric componentMetric = new ComponentMetric(streamlineComponentName, map);
		metricMap.put("COMPONENT_ID", componentMetric);
		return metricMap;
	}

	private Map<String, Object> toTaskMap(List<Object> tasks) {
		Map<String, Object> map = new HashMap<>();
		if (tasks != null) {
			for(int i=0; i<tasks.size(); i++) {
				Map task = (Map) tasks.get(i);
				if (task.get("task_id") != null) {
					map.put((String)task.get("task_id"), task);
				}
			}
		}
		return map;
	}

	public Map getExecution(TopologyLayout topology, Collection<? extends TopologyComponent> components,
							String applicationId, String executionDate) {

		List<Object> componentMetrics = new ArrayList<>();

		Map<String, Object> result = new HashMap<>();
		Map response = this.client.getTaskGraph(applicationId, executionDate);
		Map graph = (Map) response.get("graph");
		List tasks = (List)graph.get("nodes");
		Map<String, Object> taskMap = toTaskMap(tasks);

		if (components != null) {
			for (TopologyComponent component: components) {

				String componentName = component.getName();
				Map task = (Map) taskMap.getOrDefault(componentName, new HashMap());

				Map<String, Object> componentMetric = new HashMap<>();

				componentMetric.put("componentId", component.getId());
				componentMetric.put("executionDate", executionDate);
				componentMetric.put("taskStatus", task.get("state"));
				componentMetric.put("taskStartDate", task.get("start_date"));
				componentMetric.put("taskEndDate", task.get("end_date"));
				componentMetric.put("taskDuration", task.get("duration"));
				componentMetric.put("taskRetryCount", task.get("try_number"));
				componentMetric.put("taskRetries", task.get("retries"));
				componentMetric.put("taskPool", task.get("pool"));

				componentMetrics.add(componentMetric);
			}
		}

		result.put("components", componentMetrics);

		return result;

	}

	public Map getExecutions(TopologyLayout topology, String applicationId,
							 Long from, Long to, Integer page, Integer pageSize) {

		Map<String, Object> result = new HashMap<>();
		ArrayList<Object> topologyMetrics = new ArrayList<Object>();

		Map response = this.client.getPipelineRuns(applicationId, from, to, page, pageSize);
		ArrayList<Object> executions = (ArrayList<Object>) response.get("data");

		result.put("totalResults", response.get("total_results"));
		result.put("page", response.get("page"));
		result.put("pageSize", response.get("page_size"));

		if (executions != null) {
			for (int i = 0; i < executions.size(); i++) {
				Map execution = (Map) executions.get(i);

				//Abbreviated topology metric
				Map<String, Object> topologyMetric = new HashMap<>();
				topologyMetric.put("status", execution.get("state"));
				topologyMetric.put("executionDate", execution.get("execution_date"));
				topologyMetric.put("createdAt", execution.get("created_at"));
				topologyMetrics.add(topologyMetric);
			}
		}
		result.put("executions", topologyMetrics);
		return result;
	}
}
