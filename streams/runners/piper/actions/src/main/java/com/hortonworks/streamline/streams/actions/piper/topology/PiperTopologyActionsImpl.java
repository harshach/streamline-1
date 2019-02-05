package com.hortonworks.streamline.streams.actions.piper.topology;

import com.hortonworks.streamline.streams.actions.StatusImpl;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.catalog.TopologyDeployment;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.piper.common.PiperConstants;
import com.hortonworks.streamline.streams.piper.common.PiperUtil;
import com.hortonworks.streamline.streams.piper.common.pipeline.Pipeline;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;

import com.hortonworks.streamline.streams.piper.common.PiperRestAPIClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RESPONSE_DATA;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_ROOT_URL_KEY;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_METRIC_LATEST_EXECUTION_DATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXECUTION_DATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_METRIC_LATEST_EXECUTION_STATUS;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXECUTION_STATE;


public class PiperTopologyActionsImpl implements TopologyActions {

    private static final Logger LOG = LoggerFactory.getLogger(PiperTopologyActionsImpl.class);
    private static final String PIPER_RESPONSE_APPLICATION_ID = "pipeline_id";
    private EnvironmentService environmentService;

    private PiperRestAPIClient client;

    public PiperTopologyActionsImpl() {
    }

    @Override
    public void init(Map<String, Object> conf, TopologyActionsService topologyActionsService, EnvironmentService environmentService) {
        String piperAPIRootUrl = (String)conf.get(PIPER_ROOT_URL_KEY);
        this.environmentService = environmentService;
        this.client = new PiperRestAPIClient(piperAPIRootUrl, null);
    }

    @Override
    public String setUpExtraJars(Topology topology) throws IOException {
        return null;
    }

    @Override
    public void setUpClusterArtifacts(Topology topology) throws IOException {

    }

    @Override
    public void ensureValid(TopologyDag topologyDag) {
        LOG.info("XXXXXXXXXXXXXX ensureValid() XXXXXXXXXXXXXXXXXXX");
    }

    @Override
    public List<DeployedRuntimeId> deploy(TopologyLayout topology, String mavenArtifacts, TopologyDeployment deployment,
                                          TopologyActionContext ctx, String asUser) throws Exception {
        LOG.info("XXXXXXXXXXXXXX deploy() XXXXXXXXXXXXXXXXXXX");
        List<DeployedRuntimeId> deployedRuntimeIds = new ArrayList<>();

        ManagedPipelineGenerator dagVisitor = new ManagedPipelineGenerator(topology);
        Pipeline pipeline = dagVisitor.generatePipeline();
        String piperResponse = this.client.deployPipeline(pipeline);
        String applicationId = parseApplicationId(piperResponse);
        for (Long region: deployment.getRegions()) {
            deployedRuntimeIds.add(new DeployedRuntimeId(region, applicationId));
        }
        return deployedRuntimeIds;
    }

    @Override
    public List<DeployedRuntimeId> redeploy(TopologyLayout topology, String runtimeId, TopologyDeployment deployment,
                                            TopologyActionContext ctx, String asUser) throws Exception {
        List<DeployedRuntimeId> deployedRuntimeIds = new ArrayList<>();
        ManagedPipelineGenerator dagVisitor = new ManagedPipelineGenerator(topology);
        Pipeline pipeline = dagVisitor.generatePipeline();
        pipeline.setPipelineId(runtimeId);
        String piperResponse = this.client.redeployPipeline(pipeline, runtimeId);
        String applicationId =  parseApplicationId(piperResponse);
        for (Long region: deployment.getRegions()) {
            deployedRuntimeIds.add(new DeployedRuntimeId(region, applicationId));
        }
        return deployedRuntimeIds;
    }

    @Override
    public void runTest(TopologyLayout topology, TopologyTestRunHistory testRunHistory, String mavenArtifacts, Map<String, TestRunSource> testRunSourcesForEachSource, Map<String, TestRunProcessor> testRunProcessorsForEachProcessor, Map<String, TestRunRulesProcessor> testRunRulesProcessorsForEachProcessor, Map<String, TestRunSink> testRunSinksForEachSink, Optional<Long> durationSecs) throws Exception {
        LOG.info("XXXXXXXXXXXXXX runTest() XXXXXXXXXXXXXXXXXXX");
    }

    @Override
    public boolean killTest(TopologyTestRunHistory testRunHistory) {
        LOG.info("XXXXXXXXXXXXXX killTest() XXXXXXXXXXXXXXXXXXX");
        return false;
    }

    @Override
    public void kill(TopologyLayout topology, String applicationId, String asUser) throws Exception {
        LOG.info("XXXXXXXXXXXXXX kill() XXXXXXXXXXXXXXXXXXX");
        this.client.deactivatePipeline(applicationId);
    }

    @Override
    public void validate(TopologyLayout topology) throws Exception {
        LOG.info("XXXXXXXXXXXXXX validate() XXXXXXXXXXXXXXXXXXX");
    }

    @Override
    public void suspend(TopologyLayout topology, String asUser) throws Exception {
        LOG.info("XXXXXXXXXXXXXX suspend() XXXXXXXXXXXXXXXXXXX");
    }

    @Override
    public void resume(TopologyLayout topology, String asUser) throws Exception {
        LOG.info("XXXXXXXXXXXXXX resume() XXXXXXXXXXXXXXXXXXX");
    }

    @Override
    public Status status(TopologyLayout topology, String applicationId, String asUser) throws Exception {
        Map piperResponse = this.client.getPipelineState(applicationId);
        return getRuntimeStatus(piperResponse);
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
        LOG.info("XXXXXXXXXXXXXX getArtifactsLocation() XXXXXXXXXXXXXXXXXXX");
        return null;
    }

    @Override
    public Path getExtraJarsLocation(TopologyLayout topology) {
        LOG.info("XXXXXXXXXXXXXX getExtraJarsLocation() XXXXXXXXXXXXXXXXXXX");
        return null;
    }

    @Override
    public String getRuntimeTopologyId(TopologyLayout topology, String asUser) {
        LOG.info("XXXXXXXXXXXXXX getRuntimeTopologyId() XXXXXXXXXXXXXXXXXXX");
        throw new TopologyNotAliveException("Topology not found in Cluster - topology id: " + topology.getId());
    }

    private String parseApplicationId(String jsonResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject data = (JSONObject)jsonObject.get(PIPER_RESPONSE_DATA);
        return (String)data.get(PIPER_RESPONSE_APPLICATION_ID);
    }

    private Status getRuntimeStatus(Map stateResponse) {
        StatusImpl runtimeStatus = new StatusImpl();

        if (stateResponse != null) {
            String runtimeStatusString = PiperUtil.getRuntimeStatus(stateResponse);
            runtimeStatus.setStatus(runtimeStatusString);
            runtimeStatus.putExtra(PIPER_METRIC_LATEST_EXECUTION_DATE,
                    (String)stateResponse.get(STATE_KEY_EXECUTION_DATE));
            runtimeStatus.putExtra(PIPER_METRIC_LATEST_EXECUTION_STATUS,
                    (String)stateResponse.get(STATE_KEY_EXECUTION_STATE));
        }

        return runtimeStatus;
    }
}