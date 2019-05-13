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
import com.hortonworks.streamline.streams.common.athenax.AthenaxConstants;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import com.hortonworks.streamline.streams.layout.component.impl.RTASink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.registry.table.RTACreateTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTADeployTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTARestAPIClient;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hortonworks.streamline.streams.actions.athenax.topology.KafkaRTATopologyActionBuilder.UWORC_NAMESPACE_ID;
import static com.hortonworks.streamline.streams.actions.athenax.topology.KafkaRTATopologyActionBuilder.UWORC_NAMESPACE_NAME;

public class KafkaRTATopologyActionImpl implements TopologyActions {

    private RTARestAPIClient rtaRestAPIClient;
    private Long namespaceId;
    private String namespaceName;

    @Override
    public void init(Map<String, Object> conf, TopologyActionsService topologyActionsService, EnvironmentService environmentService, Subject subject) {
        rtaRestAPIClient = new RTARestAPIClient(
                (String)conf.get(Constants.CONFIG_RTA_METADATA_SERVICE_URL),
                (String)conf.get(Constants.CONFIG_RTA_METADATA_SERVICE_MUTTLEY_NAME),
                subject);

        namespaceId = (Long) conf.get(UWORC_NAMESPACE_ID);
        namespaceName = (String) conf.get(UWORC_NAMESPACE_NAME);
    }

    @Override
    public List<DeployedRuntimeId> deploy(TopologyLayout topology, String mavenArtifacts, TopologyDeployment deployment, TopologyActionContext ctx, String asUser) throws Exception {
        List<DeployedRuntimeId> deployedRuntimeIds = new ArrayList<>();
        KafkaRTAJobGraphGenerator requestGenerator = new KafkaRTAJobGraphGenerator();
        TopologyDag topologyDag = topology.getTopologyDag();
        topologyDag.traverse(requestGenerator);
        RTASink rtaSink = requestGenerator.getRtaSink();
        KafkaSource kafkaSource = requestGenerator.getKafkaSource();
        RTACreateTableRequest rtaCreateTableRequest = RTAUtils.extractRTACreateTableRequest(rtaSink, kafkaSource, asUser);
        rtaRestAPIClient.createTable(JsonClientUtil.convertRequestToJson(rtaCreateTableRequest));

        String tableName = rtaCreateTableRequest.name();
        RTADeployTableRequest rtaDeployTableRequest = RTAUtils.extractRTADeployTableRequest(kafkaSource);
        rtaRestAPIClient.deployTable(rtaDeployTableRequest, tableName);
        for (Long region: deployment.getRegions()) {
            deployedRuntimeIds.add(new DeployedRuntimeId(region, tableName));
        }
        return deployedRuntimeIds;
    }

    @Override
    public Status status(TopologyLayout topology, String applicationId, String asUser) throws Exception {
        StatusImpl status = new StatusImpl();
        String tableName = applicationId;
        String runtimeStatus;
        if (rtaRestAPIClient.getTableDeployStatus(tableName).isEmpty()) {
            runtimeStatus = AthenaxConstants.ATHENAX_RUNTIME_STATUS_INACTIVE;
        } else {
            runtimeStatus = AthenaxConstants.ATHENAX_RUNTIME_STATUS_ENABLED;
        }
        status.setStatus(runtimeStatus);
        status.setNamespaceId(namespaceId);
        status.setNamespaceName(namespaceName);
        return status;
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

    }

    @Override
    public List<DeployedRuntimeId> redeploy(TopologyLayout topology, String runtimeId, TopologyDeployment deployment, TopologyActionContext ctx, String asUser) throws Exception {
        return null;
    }

    @Override
    public void runTest(TopologyLayout topology, TopologyTestRunHistory testRunHistory, String mavenArtifacts, Map<String, TestRunSource> testRunSourcesForEachSource, Map<String, TestRunProcessor> testRunProcessorsForEachProcessor, Map<String, TestRunRulesProcessor> testRunRulesProcessorsForEachProcessor, Map<String, TestRunSink> testRunSinksForEachSink, Optional<Long> durationSecs) throws Exception {

    }

    @Override
    public boolean killTest(TopologyTestRunHistory testRunHistory) {
        return false;
    }

    @Override
    public void kill(TopologyLayout topology, String applicationId, String asUser) throws Exception {

    }

    @Override
    public void validate(TopologyLayout topology) throws Exception {

    }

    @Override
    public void suspend(TopologyLayout topology, String asUser) throws Exception {

    }

    @Override
    public void resume(TopologyLayout topology, String asUser) throws Exception {

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
        return null;
    }

    @Override
    public Path getExtraJarsLocation(TopologyLayout topology) {
        return null;
    }

    @Override
    public String getRuntimeTopologyId(TopologyLayout topology, String asUser) {
        return null;
    }
}
