package com.hortonworks.streamline.streams.actions.piper.topology;

import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.builder.TopologyActionsBuilder;
import com.hortonworks.streamline.streams.actions.common.ServiceUtils;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.piper.common.PiperUtil;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.hortonworks.streamline.streams.piper.common.PiperConstants.*;

/**
 * ToplogyActionsBuilder for PIPER implementation
 */
public class PiperTopologyActionsBuilder implements TopologyActionsBuilder<Map<String, Object>> {

    private TopologyActions piperTopologyActions;
    private TopologyActionsService topologyActionsService;
    private Map<String, Object> conf;

    @Override
    public void init(Map<String, String> streamlineConf, Engine engine, TopologyActionsService topologyActionsService,
                     Namespace namespace, Subject subject) throws Exception {
        this.conf = new HashMap<>();
        this.topologyActionsService = topologyActionsService;
        buildPiperTopologyActionsConfigMap(namespace, subject);
        piperTopologyActions = new PiperTopologyActionsImpl();
        piperTopologyActions.init(this.conf, topologyActionsService);
    }

    private void buildPiperTopologyActionsConfigMap(Namespace namespace, Subject subject) {
        EnvironmentService environmentService = topologyActionsService.getEnvironmentService();
        Service engineService = ServiceUtils.getFirstOccurenceServiceForNamespace(namespace, PIPER_SERVICE_NAME,
                topologyActionsService.getEnvironmentService());

        if (engineService == null) {
            throw new IllegalStateException("Engine " + PIPER_SERVICE_NAME + " is not associated to the namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }
        ServiceConfiguration piper = ServiceUtils.getServiceConfiguration(engineService, PIPER_SERVICE_CONFIG_NAME,
                environmentService).orElse(new ServiceConfiguration());
        Map<String, String> configMap = null;
        try {
            configMap = piper.getConfigurationMap();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String host = configMap.get(PIPER_SERVICE_CONFIG_KEY_HOST);
        String port = configMap.get(PIPER_SERVICE_CONFIG_KEY_PORT);
        String rootUrl = PiperUtil.buildPiperRestApiRootUrl(host, port);
        conf.put(PIPER_ROOT_URL_KEY, rootUrl);
    }

    @Override
    public TopologyActions getTopologyActions() {
        return piperTopologyActions;
    }

    @Override
    public Map<String, Object> getConfig() {
        return conf;
    }

    @Override
    public void cleanup() {

    }

}
