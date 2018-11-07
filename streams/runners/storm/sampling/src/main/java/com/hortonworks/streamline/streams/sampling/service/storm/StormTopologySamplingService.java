package com.hortonworks.streamline.streams.sampling.service.storm;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.catalog.Engine;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyCatalogHelperService;
import com.hortonworks.streamline.streams.sampling.service.TopologySampling;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIClient;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.Collection;
import java.util.Map;

public class StormTopologySamplingService implements TopologySampling {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologySamplingService.class);
    public static final String COMPONENT_NAME_STORM_UI_SERVER = ComponentPropertyPattern.STORM_UI_SERVER.name();

    private Engine engine;
    private Namespace namespace;
    private TopologyCatalogHelperService topologyCatalogHelperService;
    private Subject subject;
    private StormRestAPIClient client;

    public StormTopologySamplingService() {
    }

    @Override
    public void init(Engine engine, Namespace namespace, TopologyCatalogHelperService topologyCatalogHelperService, Subject subject, Map<String, Object> conf) {
        this.engine = engine;
        this.namespace = namespace;
        this.topologyCatalogHelperService = topologyCatalogHelperService;
        this.subject = subject;
        String stormApiRootUrl = null;

        if (conf != null) {
            stormApiRootUrl = (String) conf.get(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY);
            subject = (Subject) conf.get(TopologyLayoutConstants.SUBJECT_OBJECT);
        }
        Client restClient = ClientBuilder.newClient(new ClientConfig());
        this.client = new StormRestAPIClient(restClient, stormApiRootUrl, subject);
    }

    @Override
    public boolean enableSampling(Topology topology, int pct, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        return client.enableSampling(topologyId, pct, asUser);
    }

    @Override
    public boolean enableSampling(Topology topology, TopologyComponent component, int pct, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        String stormComponentId = StormTopologyUtil.generateStormComponentId(component.getId(), component.getName());
        return client.enableSampling(topologyId, stormComponentId, pct, asUser);
    }

    @Override
    public boolean disableSampling(Topology topology, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        return client.disableSampling(topologyId, asUser);
    }

    @Override
    public boolean disableSampling(Topology topology, TopologyComponent component, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        String stormComponentId = StormTopologyUtil.generateStormComponentId(component.getId(), component.getName());
        return client.disableSampling(topologyId, stormComponentId, asUser);
    }

    @Override
    public SamplingStatus getSamplingStatus(Topology topology, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (topologyId == null) {
            return null;
        }
        return buildSamplingStatus(client.getSamplingStatus(topologyId, asUser));
    }

    @Override
    public SamplingStatus getSamplingStatus(Topology topology, TopologyComponent component, String asUser) {
        String topologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (topologyId == null) {
            return null;
        }
        String stormComponentId = StormTopologyUtil.generateStormComponentId(component.getId(), component.getName());
        return buildSamplingStatus(client.getSamplingStatus(topologyId, stormComponentId, asUser));
    }

    private SamplingStatus buildSamplingStatus(Map result) {
        return result == null ? null : new SamplingStatus() {
            @Override
            public Boolean getEnabled() {
                Object debug = result.get("debug");
                return debug != null && debug instanceof Boolean ? (Boolean) debug : false;
            }

            @Override
            public Integer getPct() {
                Object samplingPct = result.get("samplingPct");
                return samplingPct != null && samplingPct instanceof Number ? ((Number) samplingPct).intValue() : 0;
            }
        };
    }

    private String buildStormRestApiRootUrl() throws ConfigException {
        // Assuming that a namespace has one mapping of engine
        Service engineService = topologyCatalogHelperService.getFirstOccurenceServiceForNamespace(namespace, engine.getName());
        if (engineService == null) {
            throw new ConfigException("Engine " + engine + " is not associated to the namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }
        com.hortonworks.streamline.streams.cluster.catalog.Component uiServer = topologyCatalogHelperService.getComponent(engineService, COMPONENT_NAME_STORM_UI_SERVER)
                .orElseThrow(() -> new RuntimeException(engine + " doesn't have " + COMPONENT_NAME_STORM_UI_SERVER + " as component"));
        Collection<ComponentProcess> uiServerProcesses = topologyCatalogHelperService.listComponentProcesses(uiServer.getId());
        if (uiServerProcesses.isEmpty()) {
            throw new ConfigException(engine + " doesn't have any process for " + COMPONENT_NAME_STORM_UI_SERVER + " as component");
        }
        ComponentProcess uiServerProcess = uiServerProcesses.iterator().next();
        String uiHost = uiServerProcess.getHost();
        Integer uiPort = uiServerProcess.getPort();
        assertHostAndPort(uiServer.getName(), uiHost, uiPort);
        return "http://" + uiHost + ":" + uiPort + "/api/v1";

    }

    private void assertHostAndPort(String componentName, String host, Integer port) {
        if (host == null || host.isEmpty() || port == null) {
            throw new RuntimeException(componentName + " component doesn't have enough information - host: " + host +
                    " / port: " + port);
        }
    }
}
