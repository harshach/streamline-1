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
package com.hortonworks.streamline.streams.actions.topology.state;

import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.storage.exception.IgnoreTransactionRollbackException;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.catalog.CatalogToLayoutConverter;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class captures the different states a 'topology' can be in and the state transitions.
 *
 * This follows the State pattern approach as described in GOF design patterns.
 */
public class DefaultTopologyStateMachine implements TopologyStateMachine {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultTopologyStateMachine.class);

    private TopologyState TOPOLOGY_STATE_INITIAL = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Constructing topology DAG");
                Topology topology = context.getTopology();
                TopologyDag dag = topology.getTopologyDag();
                topology.setTopologyDag(dag);
                context.setState(dagConstructedState());
                context.setCurrentAction("DAG constructed");
            } catch (Exception ex) {
                context.setState(deploymentFailedState());
                context.setCurrentAction("Topology DAG construction failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        @Override
        public String getName() {
            return "TOPOLOGY_STATE_INITIAL";
        }
    };

    private TopologyState TOPOLOGY_STATE_DAG_CONSTRUCTED = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Validating topology DAG");
                TopologyDag dag = context.getTopology().getTopologyDag();
                context.getTopologyActions().ensureValid(dag);
                context.setState(dagValidatedState());
                context.setCurrentAction("Topology DAG validated");
            } catch (Exception ex) {
                context.setState(deploymentFailedState());
                context.setCurrentAction("Topology DAG validation failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        @Override
        public String getName() {
            return "TOPOLOGY_STATE_DAG_CONSTRUCTED";
        }
    };

    private TopologyState TOPOLOGY_STATE_DAG_VALIDATED = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Setting up cluster artifacts");
                Topology topology = context.getTopology();
                TopologyActions topologyActions = context.getTopologyActions();
                topologyActions.setUpClusterArtifacts(topology);
                context.setState(clusterArtifactsSetupState());
                context.setCurrentAction("Cluster artifacts set up");
            } catch (Exception ex) {
                LOG.error("Error while setting up cluster artifacts", ex);
                context.setState(deploymentFailedState());
                context.setCurrentAction("Cluster artifacts set up failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        @Override
        public String getName() {
            return "TOPOLOGY_STATE_DAG_VALIDATED";
        }
    };

    private TopologyState TOPOLOGY_STATE_CLUSTER_ARTIFACTS_SETUP = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Setting up extra jars");
                Topology topology = context.getTopology();
                TopologyActions topologyActions = context.getTopologyActions();
                String mavenArtifacts = topologyActions.setUpExtraJars(topology);
                context.setMavenArtifacts(mavenArtifacts);
                context.setState(extraJarsSetupState());
                context.setCurrentAction("Extra jars set up");
            } catch (Exception ex) {
                LOG.error("Error while setting up extra jars", ex);
                context.setState(deploymentFailedState());
                context.setCurrentAction("Extra jars setup failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        @Override
        public String getName() {
            return "TOPOLOGY_STATE_CLUSTER_ARTIFACTS_SETUP";
        }
    };

    private TopologyState TOPOLOGY_STATE_EXTRA_JARS_SETUP = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            TopologyActions topologyActions = context.getTopologyActions();
            Topology topology = context.getTopology();
            TopologyDag dag = topology.getTopologyDag();
            TopologyLayout layout = CatalogToLayoutConverter.getTopologyLayout(topology, dag);
            if (dag == null) {
                throw new IllegalStateException("Topology dag not set up");
            }

            String applicationId = null;
            try {
                context.setCurrentAction("Submitting topology to streaming engine");
                String mavenArtifacts = context.getMavenArtifacts();
                applicationId = topologyActions.deploy(layout, mavenArtifacts, context, context.getAsUser());
                context.storeRuntimeApplicationId(applicationId);
                context.setState(deployedState());
                context.setCurrentAction("Topology deployed");
            } catch (Exception ex) {
                LOG.error("Error while trying to deploy the topology in the streaming engine", ex);
                LOG.error("Trying to kill any running instance of topology '{}'", context.getTopology().getName());
                killTopologyIfRunning(context, applicationId, layout);
                context.setState(deploymentFailedState());
                context.setCurrentAction("Topology submission failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        private void killTopologyIfRunning(TopologyContext context, String applicationId, TopologyLayout layout) {
            try {
                TopologyActions.Status engineStatus = context.getTopologyActions().status(layout, applicationId, context.getAsUser());
                if (!engineStatus.getStatus().equals(TopologyActions.Status.STATUS_UNKNOWN)) {
                    invokeKill(context);
                }
            } catch (Exception e) {
                LOG.debug("Not able to get running status of topology '{}'", context.getTopology().getName());
            }
        }

        @Override
        public String getName() {
            return "TOPOLOGY_STATE_EXTRA_JARS_SETUP";
        }
    };

    private TopologyState TOPOLOGY_STATE_DEPLOYED = new TopologyState() {
        @Override
        public void kill(TopologyContext context) throws Exception {
            doKill(context);
        }

        @Override
        public void suspend(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Suspending topology");
                Topology topology = context.getTopology();
                TopologyActions topologyActions = context.getTopologyActions();
                topologyActions.suspend(CatalogToLayoutConverter.getTopologyLayout(topology), context.getAsUser());
                context.setState(suspendedState());
                context.setCurrentAction("Topology suspended");
            } catch (Exception ex) {
                LOG.error("Error while trying to suspend the topology", ex);
                context.setCurrentAction("Suspending the topology failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        @Override
        public void redeploy(TopologyContext context, String runtimeId) throws Exception {

            Topology topology = context.getTopology();
            TopologyActions topologyActions = context.getTopologyActions();
            TopologyDag dag = topology.getTopologyDag();
            TopologyLayout layout = CatalogToLayoutConverter.getTopologyLayout(topology, dag);
            if (dag == null) {
                throw new IllegalStateException("Topology dag not set up");
            }

            try {
                context.setCurrentAction("Redeploying topology");
                topologyActions.redeploy(layout, runtimeId, context.getAsUser());
                context.setCurrentAction("Topology Redeployed");
            } catch (Exception ex) {
                LOG.error("Error while trying to redeploy the topology", ex);
                context.setCurrentAction("Redeploying the topology failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        @Override
        public String getName() {
            return "TOPOLOGY_STATE_DEPLOYED";
        }
    };

    private TopologyState TOPOLOGY_STATE_SUSPENDED = new TopologyState() {
        @Override
        public void kill(TopologyContext context) throws Exception {
            doKill(context);
        }

        @Override
        public void resume(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Resuming topology");
                Topology topology = context.getTopology();
                TopologyActions topologyActions = context.getTopologyActions();
                topologyActions.resume(CatalogToLayoutConverter.getTopologyLayout(topology), context.getAsUser());
                context.setState(deployedState());
                context.setCurrentAction("Topology resumed");
            } catch (Exception ex) {
                LOG.error("Error while trying to resume the topology", ex);
                context.setCurrentAction("Resuming the topology failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        @Override
        public String getName() {
            return "TOPOLOGY_STATE_SUSPENDED";
        }
    };

    // deployment error state from where we can attempt to redeploy
    private TopologyState TOPOLOGY_STATE_DEPLOYMENT_FAILED = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            context.setState(initialState());
            context.setCurrentAction("Redeploying");
        }

        @Override
        public String getName() {
            return "TOPOLOGY_STATE_DEPLOYMENT_FAILED";
        }
    };

    private void doKill(TopologyContext context) throws Exception {
        try {
            context.setCurrentAction("Killing topology");
            invokeKill(context);
            context.setState(initialState());
            context.setCurrentAction("Topology killed");
        } catch (TopologyNotAliveException ex) {
            LOG.warn("Got TopologyNotAliveException while trying to kill topology, " +
                    "probably the topology was killed externally.");
            context.setState(initialState());
            context.setCurrentAction("Setting topology to initial state since its not alive in the cluster");
        } catch (Exception ex) {
            LOG.error("Error while trying to kill the topology", ex);
            context.setCurrentAction("Killing the topology failed due to: " + ex);
            throw new IgnoreTransactionRollbackException(ex);
        }
    }

    private static void invokeKill(TopologyContext context) throws Exception {
        Topology topology = context.getTopology();
        String runAsUser = context.getAsUser();
        String applicationId = context.getTopologyActionsService().getRuntimeTopologyId(topology, runAsUser);

        TopologyActions topologyActions = context.getTopologyActions();
        topologyActions.kill(CatalogToLayoutConverter.getTopologyLayout(topology), applicationId, runAsUser);
        LOG.debug("Killed topology='{}' applicationId='{}'", topology.getName(), applicationId);
    }

    @Override
    public TopologyState initialState() {
        return TOPOLOGY_STATE_INITIAL;
    }

    @Override
    public TopologyState deployedState() {
        return TOPOLOGY_STATE_DEPLOYED;
    }

    // --------------------------------------------------------------------------
    // intermediate states between initial and deployed state
    // sub-classes can override this to plug-in new state or
    // ignore some state transitions.
    // --------------------------------------------------------------------------

    protected TopologyState dagConstructedState() {
        return TOPOLOGY_STATE_DAG_CONSTRUCTED;
    }

    protected TopologyState deploymentFailedState() {
        return TOPOLOGY_STATE_DEPLOYMENT_FAILED;
    }

    protected TopologyState dagValidatedState() {
      return TOPOLOGY_STATE_DAG_VALIDATED;
    }

    protected TopologyState clusterArtifactsSetupState() {
      return TOPOLOGY_STATE_CLUSTER_ARTIFACTS_SETUP;
    }

    protected TopologyState extraJarsSetupState() {
      return TOPOLOGY_STATE_EXTRA_JARS_SETUP;
    }

    protected TopologyState suspendedState() {
      return TOPOLOGY_STATE_SUSPENDED;
    }

    private List<TopologyState> allStates = Arrays.asList(
            TOPOLOGY_STATE_INITIAL,
            TOPOLOGY_STATE_DEPLOYED,
            TOPOLOGY_STATE_DAG_CONSTRUCTED,
            TOPOLOGY_STATE_DEPLOYMENT_FAILED,
            TOPOLOGY_STATE_DAG_VALIDATED,
            TOPOLOGY_STATE_CLUSTER_ARTIFACTS_SETUP,
            TOPOLOGY_STATE_EXTRA_JARS_SETUP,
            TOPOLOGY_STATE_SUSPENDED
    );

    @Override
    public Collection<TopologyState> allStates() {
        return allStates;
    }

    private final Map<String, TopologyState> topologyStateMap = new HashMap<>();

    public DefaultTopologyStateMachine() {
        for (TopologyState state : allStates()) {
            String name = state.getName();
            topologyStateMap.put(name, state);
            LOG.debug("Registered topology state {}", name);
        }
    }

    @Override
    public TopologyState getTopologyState(String stateName) {
        Utils.requireNonEmpty(stateName, "State name cannot be empty");
        TopologyState state = topologyStateMap.get(stateName);
        if (state == null) {
            throw new IllegalArgumentException("No such state " + stateName);
        }
        return state;
    }
}
