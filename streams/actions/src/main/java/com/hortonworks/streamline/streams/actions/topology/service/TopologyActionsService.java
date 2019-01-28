/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/

package com.hortonworks.streamline.streams.actions.topology.service;


import com.hortonworks.streamline.common.transaction.TransactionIsolation;
import com.hortonworks.streamline.common.util.FileStorage;
import com.hortonworks.streamline.storage.TransactionManager;
import com.hortonworks.streamline.storage.transaction.ManagedTransaction;
import com.hortonworks.streamline.registries.model.client.MLModelRegistryClient;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.TopologyActionsFactory;
import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;
import com.hortonworks.streamline.streams.actions.topology.state.TopologyContext;
import com.hortonworks.streamline.streams.actions.topology.state.TopologyState;
import com.hortonworks.streamline.streams.actions.topology.state.TopologyStateMachine;
import com.hortonworks.streamline.streams.actions.topology.state.TopologyStateMachineFactory;
import com.hortonworks.streamline.streams.catalog.*;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.topology.component.TopologyDagBuilder;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.container.ContainingNamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.exception.FailedToKillDeployedTopologyException;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class TopologyActionsService implements ContainingNamespaceAwareContainer {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyActionsService.class);

    private static final String TOPOLOGY_TEST_RUN_RESULT_DIR = "topologyTestRunResultDir";

    private final StreamCatalogService catalogService;
    private final EnvironmentService environmentService;
    private final TopologyDagBuilder topologyDagBuilder;
    private final FileStorage fileStorage;
    private final TopologyStateMachineFactory topologyStateMachineFactory;
    private final TopologyTestRunner topologyTestRunner;
    private final ManagedTransaction managedTransaction;
    private final TopologyActionsFactory topologyActionsFactory;
    private final Map<String, String> conf;
    private final Subject subject;

    public TopologyActionsService(StreamCatalogService catalogService, EnvironmentService environmentService,
                                  FileStorage fileStorage, MLModelRegistryClient modelRegistryClient,
                                  Map<String, Object> configuration, Subject subject, TransactionManager transactionManager) {
        this.catalogService = catalogService;
        this.environmentService = environmentService;
        this.fileStorage = fileStorage;
        this.topologyDagBuilder = new TopologyDagBuilder(catalogService, modelRegistryClient);
        this.subject = subject;
        this.conf = new HashMap<>();
        for (Map.Entry<String, Object> confEntry : configuration.entrySet()) {
            Object value = confEntry.getValue();
            conf.put(confEntry.getKey(), value == null ? null : value.toString());
        }

        String topologyTestRunResultDir = conf.get(TOPOLOGY_TEST_RUN_RESULT_DIR);
        if (StringUtils.isEmpty(topologyTestRunResultDir)) {
            throw new RuntimeException("Configuration of topology test run result dir is not set.");
        }

        if (topologyTestRunResultDir.endsWith(File.separator)) {
            topologyTestRunResultDir = topologyTestRunResultDir.substring(0, topologyTestRunResultDir.length() - 1);
        }
        this.topologyActionsFactory = new TopologyActionsFactory(configuration);
        this.topologyStateMachineFactory = new TopologyStateMachineFactory(configuration);
        this.topologyTestRunner = new TopologyTestRunner(catalogService, this, topologyTestRunResultDir);
        this.managedTransaction = new ManagedTransaction(transactionManager, TransactionIsolation.DEFAULT);
    }

    public Void deployTopology(Topology topology, String asUser) throws Exception {
        LOG.info("in deployTopology");
        TopologyContext ctx = managedTransaction.executeFunction(() -> getTopologyContext(topology, asUser));
        topology.setTopologyDag(topologyDagBuilder.getDag(topology));
        LOG.debug("Deploying topology {}", topology);

        String runtimeId = getRuntimeTopologyId(topology, asUser);

        TopologyStateMachine topologyStateMachine = getTopologyStateMachineInstance(topology);
        if (runtimeId != null && ctx.getState().equals(topologyStateMachine.deployedState())) {
            return redeployTopology(topology, asUser);
        }

        while (!ctx.getState().equals(topologyStateMachine.deployedState())) {
            managedTransaction.executeConsumer((topologyContext) -> {
                LOG.debug("Current state {}", topologyContext.getStateName());
                topologyContext.deploy();
            }, ctx);
        }
        return null;
    }

    public Void redeployTopology(Topology topology, String asUser) throws Exception {
        managedTransaction.executeConsumer(TopologyContext::redeploy, getTopologyContext(topology, asUser));
        return null;
    }

    public TopologyTestRunHistory testRunTopology(Topology topology, TopologyTestRunCase testCase, Long durationSecs) throws Exception {
        TopologyDag dag = topologyDagBuilder.getDag(topology);
        topology.setTopologyDag(dag);
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        topologyActions.ensureValid(dag);
        LOG.debug("Running topology {} in test mode", topology);
        return topologyTestRunner.runTest(topologyActions, topology, testCase, durationSecs);
    }

    public boolean killTestRunTopology(Topology topology, TopologyTestRunHistory history) {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyTestRunner.killTest(topologyActions, history);
    }

    public void killTopology(Topology topology, String asUser) throws FailedToKillDeployedTopologyException  {
        try {
            managedTransaction.executeConsumer(TopologyContext::kill, getTopologyContext(topology, asUser));
        } catch(Exception e) {
            throw new FailedToKillDeployedTopologyException(e);
        }
    }

    public void suspendTopology(Topology topology, String asUser) throws Exception {
        managedTransaction.executeConsumer(TopologyContext::suspend, getTopologyContext(topology, asUser));
    }

    public void resumeTopology(Topology topology, String asUser) throws Exception {
        managedTransaction.executeConsumer(TopologyContext::resume, getTopologyContext(topology, asUser));
    }

    public TopologyActions.Status topologyStatus(Topology topology, String asUser) throws Exception {
        String applicationId =  this.getRuntimeTopologyId(topology, asUser);
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyActions.status(CatalogToLayoutConverter.getTopologyLayout(topology), applicationId, asUser);
    }

    public String getRuntimeTopologyId(Topology topology, String asUser) throws IOException {
        TopologyRuntimeIdMap topologyRuntimeIdMap = catalogService.getTopologyRuntimeIdMap(topology.getId(), topology.getNamespaceId());
        return topologyRuntimeIdMap != null ? topologyRuntimeIdMap.getApplicationId() : null;
    }

    public TopologyActions.LogLevelInformation configureLogLevel(Topology topology, TopologyActions.LogLevel targetLogLevel, int durationSecs,
                                  String asUser) throws Exception {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyActions.configureLogLevel(CatalogToLayoutConverter.getTopologyLayout(topology), targetLogLevel,
                durationSecs, asUser);
    }

    public TopologyActions.LogLevelInformation getLogLevel(Topology topology, String asUser) throws Exception {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyActions.getLogLevel(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);
    }

    public void storeRuntimeApplicationId(Topology topology, String applicationId) throws Exception {
        catalogService.addOrUpdateTopologyRuntimeIdMap(topology, applicationId);
    }

    public StreamCatalogService getCatalogService() {
        return catalogService;
    }

    public EnvironmentService getEnvironmentService() { return environmentService; }

    public FileStorage getFileStorage() { return fileStorage; }

    @Override
    public void invalidateInstance(Long namespaceId) { }

    private TopologyActions getTopologyActionsInstance(Topology topology) {
        Namespace namespace = environmentService.getNamespace(topology.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
        }
        Engine engine = catalogService.getEngine(topology.getEngineId());
        TopologyActionsBuilder topologyActionsBuilder = topologyActionsFactory.getTopologyActionsBuilder(engine, namespace,
                this, conf, subject);
        return topologyActionsBuilder.getTopologyActions();
    }

    private TopologyStateMachine getTopologyStateMachineInstance(Topology topology) {
        Engine engine = catalogService.getEngine(topology.getEngineId());
        return topologyStateMachineFactory.getTopologyStateMachine(engine);
    }

    private TopologyContext getTopologyContext(Topology topology, String asUser) {
        TopologyStateMachine topologyStateMachine = getTopologyStateMachineInstance(topology);
        TopologyState state = catalogService
                .getTopologyState(topology.getId())
                .map(s -> topologyStateMachine.getTopologyState(s.getName()))
                .orElse(topologyStateMachine.initialState());
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return new TopologyContext(topology, topologyActions, this, state, asUser);
    }
}
