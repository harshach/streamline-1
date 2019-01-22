package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.streams.actions.StatusImpl;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.common.athenax.AthenaXRestAPIClient;
import com.hortonworks.streamline.streams.common.athenax.entity.DeployRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.JobDefinition;
import com.hortonworks.streamline.streams.common.athenax.entity.JobStatusRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.StopJobRequest;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.registry.table.RTACreateTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTADeployTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTARestAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static com.hortonworks.streamline.streams.cluster.Constants.Kafka.PROPERTY_KEY_ZOOKEEPER_CONNECT;

/**
 * AthenaX implementation of the TopologyActions interface
 */
public class AthenaxTopologyActionsImpl implements TopologyActions {

	private static final Logger LOG = LoggerFactory.getLogger(AthenaxTopologyActionsImpl.class);
	private static final String YARN_APPLICATION_STATE = "yarnApplicationState";
	private static final String FINAL_APPLICATION_STATUS = "finalApplicationStatus";
	private static final String FINISH_TIME = "finishTime";

	private AthenaXRestAPIClient athenaXRestAPIClient;
	private RTARestAPIClient rtaRestAPIClient;
	private String yarnDataCenter;
	private String yarnCluster;
	private String zkConnectionStr;

	@Override
	public void init(Map<String, Object> conf, TopologyActionsService topologyActionsService) {
		String athenaxServiceRootUrl = (String) conf.get(AthenaxConstants.ATHENAX_SERVICE_ROOT_URL_KEY);
		athenaXRestAPIClient = new AthenaXRestAPIClient(athenaxServiceRootUrl, null/*subject*/);
		rtaRestAPIClient = new RTARestAPIClient((String)conf.get(Constants.CONFIG_RTA_METADATA_SERVICE_URL), null);

		yarnDataCenter = (String) conf.get(AthenaxConstants.ATHENAX_YARN_DATA_CENTER_KEY);
		yarnCluster = (String) conf.get(AthenaxConstants.ATHENAX_YARN_CLUSTER_KEY);
		zkConnectionStr = (String) conf.get(PROPERTY_KEY_ZOOKEEPER_CONNECT);
	}

  @Override
	public String deploy(TopologyLayout topology, String mavenArtifacts, TopologyActionContext ctx, String asUser) throws Exception {
		LOG.debug("Initial Topology config {}", topology.getConfig());
		AthenaxJobGraphGenerator requestGenerator = new AthenaxJobGraphGenerator(topology, asUser);
		TopologyDag topologyDag = topology.getTopologyDag();
		topologyDag.traverse(requestGenerator);

		RTACreateTableRequest rtaCreateTableRequest = requestGenerator.extractRTACreateTableRequest();
		rtaRestAPIClient.createTable(JsonClientUtil.convertRequestToJson(rtaCreateTableRequest));

		RTADeployTableRequest rtaDeployTableRequest = requestGenerator.extractRTADeployTableRequest();
		rtaRestAPIClient.deployTable(rtaDeployTableRequest, rtaCreateTableRequest.name());
		// extract AthenaX deploy job request
		DeployRequest deployRequest = requestGenerator.extractDeployJobRequest(yarnDataCenter, yarnCluster, zkConnectionStr);

		// submit job via Athenax-vm API
		return this.athenaXRestAPIClient.deployJob(JsonClientUtil.convertRequestToJson(deployRequest));
	}

  @Override
	public String redeploy(TopologyLayout topology, String runtimeId, String asUser) throws Exception {
		return "";
	}


  @Override
	public Status status(TopologyLayout topology, String applicationId, String asUser) throws Exception {
		LOG.debug("Initial Topology config {}", topology.getConfig());
		AthenaxJobGraphGenerator requestGenerator = new AthenaxJobGraphGenerator(topology, asUser);

		// extract job status request
		JobStatusRequest request = requestGenerator.extractJobStatusRequest(applicationId, yarnDataCenter, yarnCluster);

		// send request via Athenax-vm API
		String response = athenaXRestAPIClient.jobStatus(JsonClientUtil.convertRequestToJson(request));

		// convert response(JSON string) into Map
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(response, new TypeReference<Map<String, String>>(){}); ;

		// convert into Status
		StatusImpl status = new StatusImpl();
		String yarnApplicationState = AthenaxConstants.YARN_APPLICATION_STATE_UNKNOWN;
		if (map != null) {
			yarnApplicationState = (String)map.get(YARN_APPLICATION_STATE);
			status.putExtra(FINAL_APPLICATION_STATUS, (String)map.get(FINAL_APPLICATION_STATUS));
			status.putExtra(FINISH_TIME, (String)map.get(FINISH_TIME));
		}
		status.setStatus(getRuntimeStatus(yarnApplicationState));

		return status;
	}

	private String getRuntimeStatus(String yarnApplicationState) {
		String runtimeStatus;
		switch (yarnApplicationState) {
			case AthenaxConstants.YARN_APPLICATION_STATE_RUNNING:
				runtimeStatus = AthenaxConstants.ATHENAX_RUNTIME_STATUS_ENABLED;
				break;
			case AthenaxConstants.YARN_APPLICATION_STATE_FINISHED:
				runtimeStatus = AthenaxConstants.ATHENAX_RUNTIME_STATUS_INACTIVE;
				break;
			default:
				runtimeStatus = AthenaxConstants.ATHENAX_RUNTIME_STATUS_UNKNOWN;
		}
		return runtimeStatus;
	}

  @Override
	public void runTest(TopologyLayout topology, TopologyTestRunHistory testRunHistory, String mavenArtifacts, Map<String, TestRunSource> testRunSourcesForEachSource, Map<String, TestRunProcessor> testRunProcessorsForEachProcessor, Map<String, TestRunRulesProcessor> testRunRulesProcessorsForEachProcessor, Map<String, TestRunSink> testRunSinksForEachSink, Optional<Long> durationSecs) throws Exception {
		LOG.info("runTest() not implemented!");
	}

  @Override
	public boolean killTest(TopologyTestRunHistory testRunHistory) {
		LOG.info("killTest() not implemented!");
		return false;
	}

  @Override
	public void kill(TopologyLayout topology, String applicationId, String asUser) throws Exception {
		LOG.debug("Initial Topology config {}", topology.getConfig());
		AthenaxJobGraphGenerator requestGenerator = new AthenaxJobGraphGenerator(topology, asUser);

		// extract kill job request
		StopJobRequest request = requestGenerator.extractStopJobRequest(applicationId, yarnDataCenter, yarnCluster);

		// send request via Athenax-vm API
		athenaXRestAPIClient.stopJob(JsonClientUtil.convertRequestToJson(request));
	}

  @Override
	public void validate(TopologyLayout topology) throws Exception {
		LOG.debug("Initial Topology config {}", topology.getConfig());
		AthenaxJobGraphGenerator requestGenerator = new AthenaxJobGraphGenerator(topology, null);
		TopologyDag topologyDag = topology.getTopologyDag();
		topologyDag.traverse(requestGenerator);

		// extract AthenaX job description
    JobDefinition jobDef = requestGenerator.extractJobDefinition(zkConnectionStr);

		// send request via Athenax-vm API
    athenaXRestAPIClient.validateJob(JsonClientUtil.convertRequestToJson(jobDef));
	}

  @Override
	public void suspend(TopologyLayout topology, String asUser) throws Exception {
		LOG.info("suspend() not implemented!");
	}

  @Override
	public void resume(TopologyLayout topology, String asUser) throws Exception {
		LOG.info("resume() not implemented!");
	}

  @Override
	public LogLevelInformation configureLogLevel(TopologyLayout topology, LogLevel targetLogLevel, int durationSecs, String asUser) throws Exception {
		return null;
	}

  @Override
	public LogLevelInformation getLogLevel(TopologyLayout topology, String asUser) throws Exception {
		return null;
	}

  @Override
	public Path getArtifactsLocation(TopologyLayout topology) {
		LOG.info("getArtifactsLocation() not implemented!");
		return null;
	}

  @Override
	public Path getExtraJarsLocation(TopologyLayout topology) {
		LOG.info("getExtraJarsLocation() not implemented!");
		return null;
	}

  @Override
	public String getRuntimeTopologyId(TopologyLayout topology, String asUser) {
		LOG.info("getRuntimeTopologyId() not implemented!");
		return null;
	}

	@Override
	public void ensureValid(TopologyDag dag) {
		LOG.info("ensureValid() not implemented!");
	}

	@Override
	public String setUpExtraJars(Topology topology) throws IOException {
		return null;
	}
	@Override
	public void setUpClusterArtifacts(Topology topology) throws IOException {
		return;
	}
}
