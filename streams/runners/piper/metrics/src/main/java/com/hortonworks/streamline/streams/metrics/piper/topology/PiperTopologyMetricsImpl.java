package com.hortonworks.streamline.streams.metrics.piper.topology;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.piper.common.PiperRestAPIClient;
import org.apache.avro.generic.GenericData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public void init(Map<String, Object> conf) throws ConfigException {
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

	public Map getExecution(TopologyLayout topology, Collection<? extends TopologyComponent> components,
							String applicationId, String executionDate) {

		ArrayList<Object> componentMetrics = new ArrayList<Object>();

		Map result = new HashMap<>();
		Map response = this.client.getTaskGraph(applicationId, executionDate);
		Map graph = (Map) response.get("graph");
		ArrayList<Object> nodes = (ArrayList<Object>)graph.get("nodes");
		if (nodes != null) {
			for(int i=0; i<nodes.size(); i++) {
				Map componentMetric = new HashMap();
				Map node = (Map) nodes.get(i);

				componentMetric.put("componentId", i); // FIXME
				componentMetric.put("executionDate", executionDate);
				componentMetric.put("taskStatus", node.get("state"));
				componentMetric.put("taskStartDate", node.get("start_date"));
				componentMetric.put("taskEndDate", node.get("end_date"));
				componentMetric.put("taskDuration", node.get("duration"));
				componentMetric.put("taskRetryCount", node.get("try_number"));
				componentMetric.put("taskRetries", node.get("retries"));
				componentMetric.put("taskPool", node.get("pool"));

				componentMetrics.add(componentMetric);
			}
		}

		result.put("components", componentMetrics);

		return result;

	}

	public Map getExecutions(TopologyLayout topology, String applicationId,
							 Long from, Long to, Integer page, Integer pageSize) {

		Map result = new HashMap<>();
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
				Map topologyMetric = new HashMap();
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
