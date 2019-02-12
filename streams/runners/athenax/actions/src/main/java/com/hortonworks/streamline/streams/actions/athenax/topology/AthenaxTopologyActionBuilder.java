package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;
import com.hortonworks.streamline.streams.actions.common.ServiceUtils;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.common.athenax.AthenaxConstants;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AthenaxTopologyActionBuilder implements TopologyActionsBuilder<Map<String, Object>> {
    private TopologyActions athenaxTopologyActions;
    private TopologyActionsService topologyActionsService;
    private Map<String, Object> conf;

    @Override
    public void init(Map<String, String> conf, Engine engine, TopologyActionsService topologyActionsService,
                     EnvironmentService environmentService, Namespace namespace, Subject subject) throws Exception {
        this.conf = new HashMap<>();
        this.conf.put(Constants.CONFIG_RTA_METADATA_SERVICE_URL, conf.get(Constants.CONFIG_RTA_METADATA_SERVICE_URL));
        this.topologyActionsService = topologyActionsService;
        buildAthenaxTopologyActionsConfigMap(namespace);
        athenaxTopologyActions = new AthenaxTopologyActionsImpl();
        athenaxTopologyActions.init(this.conf, topologyActionsService, environmentService);
    }

    private void buildAthenaxTopologyActionsConfigMap(Namespace namespace) {
        EnvironmentService environmentService = topologyActionsService.getEnvironmentService();
        Service engineService = ServiceUtils.getFirstOccurenceServiceForNamespace(namespace, AthenaxConstants.ATHENAX_SERVICE_NAME,
                topologyActionsService.getEnvironmentService());

        if (engineService == null) {
            throw new IllegalStateException("Engine " + AthenaxConstants.ATHENAX_SERVICE_NAME +
                    " is not associated to the namespace " + namespace.getName() + "(" + namespace.getId() + ")");
        }
        ServiceConfiguration serviceConfiguration = ServiceUtils.getServiceConfiguration(engineService,
                AthenaxConstants.ATHENAX_SERVICE_CONFIG_NAME, environmentService).orElse(new ServiceConfiguration());
        Map<String, String> configMap;
        try {
            configMap = serviceConfiguration.getConfigurationMap();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        conf.putAll(configMap);

        String host = configMap.get(AthenaxConstants.ATHENAX_SERVICE_HOST_KEY);
        String port = configMap.get(AthenaxConstants.ATHENAX_SERVICE_PORT_KEY);
        String rootUrl = "http://" + host + ":" + port;
        conf.put(AthenaxConstants.ATHENAX_SERVICE_ROOT_URL_KEY, rootUrl);
    }

    @Override
    public TopologyActions getTopologyActions() {
        return athenaxTopologyActions;
    }

    @Override
    public Map<String, Object> getConfig() {
        return conf;
    }

    @Override
    public void cleanup() {

    }
}
