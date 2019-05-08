package com.hortonworks.streamline.streams.actions.piper.topology;

import com.hortonworks.streamline.streams.actions.StatusImpl;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.catalog.TopologyDeployment;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.register.impl.PiperServiceRegistrar;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hortonworks.streamline.streams.actions.piper.topology.ManagedPipelineGenerator.PIPER_CONFIG_DATACENTER_CHOICE_MODE;
import static com.hortonworks.streamline.streams.actions.piper.topology.ManagedPipelineGenerator.PIPER_CONFIG_RUN_ALL_DATACENTERS;
import static com.hortonworks.streamline.streams.actions.piper.topology.ManagedPipelineGenerator.PIPER_CONFIG_SELECTED_DATACENTERS;
import static com.hortonworks.streamline.streams.actions.piper.topology.ManagedPipelineGenerator.PIPER_EXECUTION_DATE_FORMAT;
import static com.hortonworks.streamline.streams.actions.piper.topology.PiperTopologyActionsBuilder.UWORC_NAMESPACE_ID;
import static com.hortonworks.streamline.streams.actions.piper.topology.PiperTopologyActionsBuilder.UWORC_NAMESPACE_NAME;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_METRIC_START_EXECUTION_DATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_OFFLINE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_PIPELINE_EXECUTION_INTERVAL_UNIT;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_PIPELINE_EXECUTION_INTERVAL_VALUE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RESPONSE_DATA;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RESPONSE_METADATA;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_ROOT_URL_KEY;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_UI_ROOT_URL_KEY;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_METRIC_LATEST_EXECUTION_DATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXECUTION_DATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_METRIC_LATEST_EXECUTION_STATUS;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXECUTION_STATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXTRAS;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_NTH_EXECUTION_DATE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_NUMBER_OF_EXECUTIONS;

public class PiperTopologyActionsImpl implements TopologyActions {

    private static final Logger LOG = LoggerFactory.getLogger(PiperTopologyActionsImpl.class);
    private static final String PIPER_RESPONSE_APPLICATION_ID = "pipeline_id";
    private static final long MILLISECONDS = 1000;
    private static final long ONE_DAY_IN_SECONDS = 86400;
    private EnvironmentService environmentService;
    private TopologyActionsService actionsService;
    private Long namespaceId;
    private String namespaceName;
    private String piperUIRootUrl;

    private PiperRestAPIClient client;

    public PiperTopologyActionsImpl() {
    }

    @Override
    public void init(Map<String, Object> conf, TopologyActionsService topologyActionsService, EnvironmentService environmentService, Subject subject) {
        String piperAPIRootUrl = (String)conf.get(PIPER_ROOT_URL_KEY);
        this.namespaceId = (Long) conf.get(UWORC_NAMESPACE_ID);
        this.namespaceName = (String) conf.get(UWORC_NAMESPACE_NAME);
        this.piperUIRootUrl = (String) conf.get(PIPER_UI_ROOT_URL_KEY);
        this.environmentService = environmentService;
        this.client = new PiperRestAPIClient(piperAPIRootUrl, subject);
        this.actionsService = topologyActionsService;
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

        Map<String, Object> piperDeployment = convertTopologyDeploymentToPiperDeployment(deployment);

        ManagedPipelineGenerator dagVisitor = new ManagedPipelineGenerator(topology, piperDeployment);
        Pipeline pipeline = dagVisitor.generatePipeline();
        String piperResponse = this.client.deployPipeline(pipeline);
        String applicationId = parseApplicationId(piperResponse);
        Map<String, Object> piperDeployed = parseDeployment(piperResponse);

        for (Long region: regionsDeployed(piperDeployed)) {
            deployedRuntimeIds.add(new DeployedRuntimeId(region, applicationId));
        }
        return deployedRuntimeIds;
    }

    @Override
    public List<DeployedRuntimeId> redeploy(TopologyLayout topology, String runtimeId, TopologyDeployment deployment,
                                            TopologyActionContext ctx, String asUser) throws Exception {
        List<DeployedRuntimeId> deployedRuntimeIds = new ArrayList<>();
        Map<String, Object> piperDeployment = convertTopologyDeploymentToPiperDeployment(deployment);
        ManagedPipelineGenerator dagVisitor = new ManagedPipelineGenerator(topology, piperDeployment);
        Pipeline pipeline = dagVisitor.generatePipeline();
        pipeline.setPipelineId(runtimeId);
        String piperResponse = this.client.redeployPipeline(pipeline, runtimeId);
        String applicationId =  parseApplicationId(piperResponse);
        Map<String, Object> piperDeployed = parseDeployment(piperResponse);

        for (Long region: regionsDeployed(piperDeployed)) {
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
        StatusImpl runtimeStatus = new StatusImpl();

        try {
            Map stateResponse = this.client.getDetailedPipelineState(applicationId);
            String runtimeStatusString = PiperUtil.getRuntimeStatus(stateResponse);
            runtimeStatus.setStatus(runtimeStatusString);
            // Setting default values for Start and End Date
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat(PIPER_EXECUTION_DATE_FORMAT);
            runtimeStatus.putExtra(PIPER_METRIC_LATEST_EXECUTION_DATE, dateFormat.format(currentDate));
            Date date = new Date(currentDate.getTime() - (ONE_DAY_IN_SECONDS * MILLISECONDS));
            runtimeStatus.putExtra(PIPER_METRIC_START_EXECUTION_DATE, dateFormat.format(date));

            if (stateResponse.keySet().contains(STATE_KEY_EXECUTION_DATE) &&
                    stateResponse.get(STATE_KEY_EXECUTION_DATE) != null &&
                    !((String)stateResponse.get(STATE_KEY_EXECUTION_DATE)).isEmpty()) {
                runtimeStatus.putExtra(PIPER_METRIC_LATEST_EXECUTION_DATE,
                        (String)stateResponse.get(STATE_KEY_EXECUTION_DATE));
            }

           runtimeStatus.putExtra(PIPER_METRIC_LATEST_EXECUTION_STATUS,
                        (String)stateResponse.get(STATE_KEY_EXECUTION_STATE));

            Map<String, Object> extras = (Map<String, Object>) stateResponse.get(STATE_KEY_EXTRAS);
            int totalExecutions = 0;
            if (extras != null) {
                if (extras.keySet().contains(STATE_KEY_NTH_EXECUTION_DATE)) {
                    runtimeStatus.putExtra(PIPER_METRIC_START_EXECUTION_DATE,
                            (String)extras.get(STATE_KEY_NTH_EXECUTION_DATE));
                }
                if (extras.keySet().contains(STATE_KEY_NUMBER_OF_EXECUTIONS)) {
                    totalExecutions = (int) extras.get(STATE_KEY_NUMBER_OF_EXECUTIONS);
                }
                if (totalExecutions > 0 ) {
                    DateFormat df = new SimpleDateFormat(PIPER_EXECUTION_DATE_FORMAT);
                    try {
                        Date endDate    = df.parse(stateResponse.get(STATE_KEY_EXECUTION_DATE).toString());
                        Date startDate  = df.parse(extras.get(STATE_KEY_NTH_EXECUTION_DATE).toString());
                        long delta = (endDate.getTime() - startDate.getTime()) / totalExecutions;
                        Map<String, String> values = getInterVal(delta / MILLISECONDS);
                        runtimeStatus.putExtra(PIPER_PIPELINE_EXECUTION_INTERVAL_VALUE, values.get(PIPER_PIPELINE_EXECUTION_INTERVAL_VALUE));
                        runtimeStatus.putExtra(PIPER_PIPELINE_EXECUTION_INTERVAL_UNIT, values.get(PIPER_PIPELINE_EXECUTION_INTERVAL_UNIT));
                    } catch (ParseException e) {
                        runtimeStatus.setException(e);
                    }
                }
            }

        } catch (RuntimeException e){
            runtimeStatus.setStatus(PIPER_OFFLINE);
            runtimeStatus.setException(e);
        }
        runtimeStatus.setDeploymentStatus(actionsService.getDeploymentState(topology.getId()));
        runtimeStatus.setNamespaceId(this.namespaceId);
        runtimeStatus.setNamespaceName(this.namespaceName);
        runtimeStatus.setRuntimeAppId(applicationId);
        runtimeStatus.setRuntimeAppUrl(getRuntimeAppUrl(applicationId));

        return runtimeStatus;
    }

    private Map<String, String> getInterVal(long delta) {
        Map<String, String> interval = new HashMap<>();
        if (delta/(60 * 60 * 24 * 7) > 0) {
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_VALUE, Long.toString(delta/(60 * 60 * 24 * 7)));
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_UNIT, "Week");
        } else if (delta/(60 * 60 * 24) > 0) {
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_VALUE, Long.toString(delta/(60 * 60 * 24)));
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_UNIT, "Day");
        } else if (delta/(60 * 60) > 0) {
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_VALUE, Long.toString(delta/(60 * 60)));
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_UNIT, "Hour");
        } else if (delta/(60) > 0) {
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_VALUE, Long.toString(delta/60));
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_UNIT, "Minute");
        } else if (delta > 0) {
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_VALUE, Long.toString(delta));
            interval.put(PIPER_PIPELINE_EXECUTION_INTERVAL_UNIT, "Second");
        }
        return interval;
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

    private Map<String, Object> parseDeployment(String jsonResponse) throws JSONException {
        Map<String, Object> deployment = new HashMap<>();
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject data = (JSONObject)jsonObject.get(PIPER_RESPONSE_DATA);
        JSONObject metadata = (JSONObject)data.get(PIPER_RESPONSE_METADATA);
        String dataCenterChoiceMode = metadata.getString(PIPER_CONFIG_DATACENTER_CHOICE_MODE);

        if (metadata.has(PIPER_CONFIG_SELECTED_DATACENTERS)) {
            JSONArray selectedDatacenters = metadata.getJSONArray(PIPER_CONFIG_SELECTED_DATACENTERS);

            List<String> deployedDatacenters = new ArrayList<>();
            for (int i = 0; i < selectedDatacenters.length(); i++) {
                String datacenter = selectedDatacenters.getString(i);
                deployedDatacenters.add(datacenter);
            }

            deployment.put(PIPER_CONFIG_SELECTED_DATACENTERS, deployedDatacenters);
        }

        deployment.put(PIPER_CONFIG_DATACENTER_CHOICE_MODE, dataCenterChoiceMode);
        return deployment;
    }

    private String getRuntimeAppUrl(String runtimeAppId) {
        String url = this.piperUIRootUrl;

        if (runtimeAppId != null) {
            url = String.format("%s/?pipeline_id=%s&search=%s&num_runs=14&expand=true",
                    this.piperUIRootUrl, runtimeAppId, runtimeAppId);
        }
        return url;
    }

    private Map<String, Object> convertTopologyDeploymentToPiperDeployment(TopologyDeployment topologyDeployment) {
        Map<String, Object> piperDeployment = new HashMap<>();

        String dcChoiceMode = PIPER_CONFIG_RUN_ALL_DATACENTERS;
        List<String> datacenters = new ArrayList<>();

        if (!topologyDeployment.getDeploymentSetting().equals(TopologyDeployment.DeploymentSetting.ALL_REGIONS)) {
            dcChoiceMode = ManagedPipelineGenerator.PIPER_CONFIG_RUN_CHOSEN_DATACENTER;

            for(Long namespaceId : topologyDeployment.getRegions()) {
                Map<String, String> map = getPiperServiceConfigurationMap(namespaceId);
                if (map == null) {
                    throw new IllegalStateException("Could not find namespace " + namespaceId);
                }
                datacenters.add(map.get(PiperServiceRegistrar.PARAM_PIPER_API_DATACENTER));
            }
            piperDeployment.put(ManagedPipelineGenerator.PIPER_TOPOLOGY_CONFIG_SELECTED_DATACENTER, datacenters);
        }

        piperDeployment.put(ManagedPipelineGenerator.PIPER_TOPOLOGY_CONFIG_DATACENTER_CHOICE_MODE, dcChoiceMode);

        return piperDeployment;
    }

    private List<Long> regionsDeployed(Map<String, Object> piperDeployment) {
        List<Long> regionsDeployed = new ArrayList<>();

        String datacenterChoiceMode = (String) piperDeployment.get(PIPER_CONFIG_DATACENTER_CHOICE_MODE);
        List<String> datacentersDeployed = new ArrayList<>();

        if (PIPER_CONFIG_RUN_ALL_DATACENTERS.equals(datacenterChoiceMode)) {
            // Piper doesn't return a list of deployed DCs, so assume
            datacentersDeployed.add("sjc1");
            datacentersDeployed.add("dca1");
            datacentersDeployed.add("phx2");
        } else if (piperDeployment.containsKey(PIPER_CONFIG_SELECTED_DATACENTERS)) {
            datacentersDeployed = (List<String>) piperDeployment.get(PIPER_CONFIG_SELECTED_DATACENTERS);
        }

        Collection<Namespace> namespaces = environmentService.listNamespaces();
        for(Namespace namespace: namespaces) {
            Map<String, String> configMap = getPiperServiceConfigurationMap(namespace.getId());
            if (configMap != null) {
                String datacenterForNamespace = configMap.get(PiperServiceRegistrar.PARAM_PIPER_API_DATACENTER);

                if (datacentersDeployed.contains(datacenterForNamespace)) {
                    regionsDeployed.add(namespace.getId());
                }
            }
        }
        return regionsDeployed;
    }

    private Map<String, String> getPiperServiceConfigurationMap(Long namespaceId) {
        Map<String, String> piperServiceConfigurationMap = null;

        Namespace namespace = environmentService.getNamespace(namespaceId);
        if (namespace != null) {
            Service piperService = environmentService.getFirstOccurenceServiceForNamespace(namespace,
                PiperServiceRegistrar.SERVICE_NAME);

            if (piperService != null) {
                ServiceConfiguration piperServiceConfig = environmentService.
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
}