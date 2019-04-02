package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.streams.actions.StatusImpl;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyDeployment;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.common.athenax.AthenaXRestAPIClient;
import com.hortonworks.streamline.streams.common.athenax.AthenaxConstants;
import com.hortonworks.streamline.streams.common.athenax.AthenaxUtils;
import com.hortonworks.streamline.streams.common.athenax.entity.DeployRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.JobDefinition;
import com.hortonworks.streamline.streams.common.athenax.entity.JobStatusRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.StopJobRequest;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.RTASink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.registry.table.RTACreateTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTADeployTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTARestAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hortonworks.streamline.streams.actions.athenax.topology.AthenaxTopologyActionBuilder.UWORC_NAMESPACE_ID;
import static com.hortonworks.streamline.streams.actions.athenax.topology.AthenaxTopologyActionBuilder.UWORC_NAMESPACE_NAME;

/**
 * AthenaX implementation of the TopologyActions interface
 */
public class AthenaxTopologyActionsImpl implements TopologyActions {

	private static final Logger LOG = LoggerFactory.getLogger(AthenaxTopologyActionsImpl.class);
	private static final String YARN_APPLICATION_STATE = "yarnApplicationState";
	private static final String FINAL_APPLICATION_STATUS = "finalApplicationStatus";
	private static final String FINISH_TIME = "finishTime";

	private EnvironmentService environmentService;
	private AthenaXRestAPIClient athenaXRestAPIClient;
	private RTARestAPIClient rtaRestAPIClient;
	private Long namespaceId;
	private String namespaceName;
	private String yarnDataCenter;
	private String yarnCluster;

	@Override
	public void init(Map<String, Object> conf, TopologyActionsService topologyActionsService, EnvironmentService environmentService, Subject subject) {
		this.environmentService = environmentService;
		String athenaxServiceRootUrl = (String) conf.get(AthenaxConstants.ATHENAX_SERVICE_ROOT_URL_KEY);
		String athenaxServiceMuttleyName = (String) conf.get(AthenaxConstants.ATHENAX_SERVICE_MUTTLEY_NAME);
		athenaXRestAPIClient = new AthenaXRestAPIClient(athenaxServiceRootUrl, athenaxServiceMuttleyName, subject);
		rtaRestAPIClient = new RTARestAPIClient(
				(String)conf.get(Constants.CONFIG_RTA_METADATA_SERVICE_URL),
				(String)conf.get(Constants.CONFIG_RTA_METADATA_SERVICE_MUTTLEY_NAME),
				subject);

		namespaceId = (Long) conf.get(UWORC_NAMESPACE_ID);
		namespaceName = (String) conf.get(UWORC_NAMESPACE_NAME);

		yarnDataCenter = (String) conf.get(AthenaxConstants.ATHENAX_YARN_DATA_CENTER_KEY);
		yarnCluster = (String) conf.get(AthenaxConstants.ATHENAX_YARN_CLUSTER_KEY);
	}

  @Override
	public List<DeployedRuntimeId> deploy(TopologyLayout topology, String mavenArtifacts, TopologyDeployment deployment,
										  TopologyActionContext ctx, String asUser) throws Exception {
		LOG.debug("Initial Topology config {}", topology.getConfig());
		List<DeployedRuntimeId> deployedRuntimeIds = new ArrayList<>();
		AthenaxJobGraphGenerator requestGenerator = new AthenaxJobGraphGenerator(topology, environmentService);
		TopologyDag topologyDag = topology.getTopologyDag();
		topologyDag.traverse(requestGenerator);

		// send requests to rta-ums for RTA connectors, if there is any
		RTACreateTableRequest rtaCreateTableRequest;
		for (RTASink rtaSink : requestGenerator.getRTASinkList()) {
			rtaCreateTableRequest = RTAUtils.extractRTACreateTableRequest(rtaSink, asUser);
			rtaRestAPIClient.createTable(JsonClientUtil.convertRequestToJson(rtaCreateTableRequest));

			String tableName = rtaCreateTableRequest.name();
			if (rtaRestAPIClient.getTableDeployStatus(tableName).isEmpty()) {
				RTADeployTableRequest rtaDeployTableRequest = RTAUtils.extractRTADeployTableRequest(rtaSink);
				rtaRestAPIClient.deployTable(rtaDeployTableRequest, tableName);
			}
		}
		// extract AthenaX deploy job request
		DeployRequest deployRequest = requestGenerator.extractDeployJobRequest(yarnDataCenter, yarnCluster);

		// submit job via Athenax-vm API
		String applicationId =  this.athenaXRestAPIClient.deployJob(deployRequest);
		for (Long region: deployment.getRegions()) {
			  deployedRuntimeIds.add(new DeployedRuntimeId(region, applicationId));
	    }
	    return deployedRuntimeIds;
	}

  @Override
	public  List<DeployedRuntimeId> redeploy(TopologyLayout topology, String runtimeId, TopologyDeployment deployment,
											 TopologyActionContext ctx, String asUser) throws Exception {
	    List<DeployedRuntimeId> deployedRuntimeIds = new ArrayList<>();
		return deployedRuntimeIds;
	}


  @Override
	public Status status(TopologyLayout topology, String applicationId, String asUser) throws Exception {
		LOG.debug("Initial Topology config {}", topology.getConfig());

		// extract job status request
		JobStatusRequest request = AthenaxUtils.extractJobStatusRequest(applicationId, yarnDataCenter, yarnCluster);

		// send request via Athenax-vm API
		Map<String, String> statusMap = athenaXRestAPIClient.jobStatus(request);

		// convert into Status
		StatusImpl status = new StatusImpl();
		String yarnApplicationState = AthenaxConstants.YARN_APPLICATION_STATE_UNKNOWN;
		if (statusMap != null) {
			yarnApplicationState = statusMap.get(YARN_APPLICATION_STATE);
			status.putExtra(FINAL_APPLICATION_STATUS, statusMap.get(FINAL_APPLICATION_STATUS));
			status.putExtra(FINISH_TIME, statusMap.get(FINISH_TIME));
		}
		status.setStatus(getRuntimeStatus(yarnApplicationState));
		status.setRuntimeAppId(applicationId);
		status.setNamespaceId(namespaceId);
		status.setNamespaceName(namespaceName);

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

		// extract kill job request
		StopJobRequest stopJobRequest = AthenaxUtils.extractStopJobRequest(applicationId, yarnDataCenter, yarnCluster);

		// send request via Athenax-vm API
		athenaXRestAPIClient.stopJob(stopJobRequest);
	}

  @Override
	public void validate(TopologyLayout topology) throws Exception {
		LOG.debug("Initial Topology config {}", topology.getConfig());
		AthenaxJobGraphGenerator requestGenerator = new AthenaxJobGraphGenerator(topology, environmentService);
		TopologyDag topologyDag = topology.getTopologyDag();
		topologyDag.traverse(requestGenerator);

		// extract AthenaX job description
		JobDefinition jobDefinition = requestGenerator.extractJobDefinition(yarnDataCenter, yarnCluster);

		// send request via Athenax-vm API
		athenaXRestAPIClient.validateJob(jobDefinition);
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
