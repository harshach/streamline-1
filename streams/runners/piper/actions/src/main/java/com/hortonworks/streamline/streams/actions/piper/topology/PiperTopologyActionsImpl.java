package com.hortonworks.streamline.streams.actions.piper.topology;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.piper.common.pipeline.Pipeline;
import com.hortonworks.streamline.streams.piper.common.pipeline.Task;
import com.hortonworks.streamline.streams.piper.common.pipeline.TaskParams;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.layout.component.InputComponent;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;

import com.hortonworks.streamline.streams.piper.common.PiperRestAPIClient;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PiperTopologyActionsImpl implements TopologyActions {

    private static final Logger LOG = LoggerFactory.getLogger(PiperTopologyActionsImpl.class);

    private PiperRestAPIClient client;

    public PiperTopologyActionsImpl() {
    }

    @Override
    public void init(Map<String, Object> conf, TopologyActionsService topologyActionsService) {
        String piperAPIRootUrl = "http://127.0.0.1:4310";
        Subject subject = null;

        this.client = new PiperRestAPIClient(piperAPIRootUrl, subject);
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
    public void deploy(TopologyLayout topology, String mavenArtifacts, TopologyActionContext ctx, String asUser) throws Exception {
        LOG.info("XXXXXXXXXXXXXX deploy() XXXXXXXXXXXXXXXXXXX");

        ManagedPipelineGenerator dagVisitor = new ManagedPipelineGenerator(topology);
        //topology.getTopologyDag().traverse(dagVisitor);

        Pipeline pipeline = dagVisitor.generatePipeline();
        PiperRestAPIClient client = new PiperRestAPIClient("http://localhost:4310", null);
        LOG.info(client.deployPipeline(pipeline));
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
    public void kill(TopologyLayout topology, String asUser) throws Exception {
        LOG.info("XXXXXXXXXXXXXX kill() XXXXXXXXXXXXXXXXXXX");
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
    public Status status(TopologyLayout topology, String asUser) throws Exception {
        LOG.info("XXXXXXXXXXXXXX status() XXXXXXXXXXXXXXXXXXX");
        return null;
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
}