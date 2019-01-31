package com.hortonworks.streamline.streams.cluster.service.metadata;

import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.cluster.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.piper.common.PiperRestAPIClient;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.security.auth.Subject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
public class PiperMetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(PiperMetadataService.class);

    public static final MediaType REST_API_MEDIA_TYPE = MediaType.TEXT_PLAIN_TYPE;

    public static final String PIPER_SERVICE_NAME = "PIPER";
    public static final String PIPER_SERVICE_CONFIG_NAME = "properties";
    public static final String PIPER_SERVICE_CONFIG_KEY_HOST = "piper.service.host";
    public static final String PIPER_SERVICE_CONFIG_KEY_PORT = "piper.service.port";
    public static final String TOTAL_RESULTS = "total_results" ;
    public static final String PAGE_SIZE = "page_size" ;


    private final PiperRestAPIClient piperRestAPIClient;

    public PiperMetadataService(
            EnvironmentService environmentService, Cluster cluster, SecurityContext securityContext, Subject subject)
                throws IOException, ServiceNotFoundException, ServiceConfigurationNotFoundException{

        ServiceConfiguration piperServiceConfig = getServiceConfig(environmentService, cluster.getId(), PIPER_SERVICE_CONFIG_NAME);
        Map<String, String> configMap = piperServiceConfig.getConfigurationMap();

        String host = configMap.get(PIPER_SERVICE_CONFIG_KEY_HOST);
        String port = configMap.get(PIPER_SERVICE_CONFIG_KEY_PORT);

        if (host == null) {
            throw new IllegalStateException(String.format("%s %s not set", PIPER_SERVICE_NAME, PIPER_SERVICE_CONFIG_KEY_HOST));
        }

        if (port == null) {
            throw new IllegalStateException(String.format("%s %s not set", PIPER_SERVICE_NAME, PIPER_SERVICE_CONFIG_KEY_PORT));
        }

        String apiRootUrl = String.format("http://%s:%s", host, port);

        this.piperRestAPIClient = new PiperRestAPIClient(apiRootUrl, subject);
    }

    private Long getPiperServiceId(EnvironmentService environmentService, Long clusterId) throws ServiceNotFoundException {
        Long serviceId = environmentService.getServiceIdByName(clusterId, PIPER_SERVICE_NAME);
        if (serviceId == null) {
            throw new ServiceNotFoundException(clusterId, PIPER_SERVICE_NAME);
        }
        return serviceId;
    }

    private ServiceConfiguration getServiceConfig(EnvironmentService environmentService, Long clusterId, String configName)
            throws ServiceNotFoundException, IOException, ServiceConfigurationNotFoundException {

        final ServiceConfiguration serviceConfig = environmentService.getServiceConfigurationByName(
                getPiperServiceId(environmentService, clusterId), configName);

        if (serviceConfig == null || serviceConfig.getConfigurationMap() == null) {
            throw new ServiceConfigurationNotFoundException(clusterId, ServiceConfigurations.KAFKA.name(),
                    configName);
        }
        return serviceConfig;
    }


    public Collection getConnections(String type) {
        Map response = this.piperRestAPIClient.getConnections(type);

        assertNotTruncated(response);

        Collection data = (Collection)response.get("data");
        Collection result = new ArrayList<String>();
        Iterator<Object> iterator = data.iterator();
        while (iterator.hasNext()) {
            Map connection = (Map)iterator.next();
            result.add(connection.get("conn_id"));
        }
        return result;
    }

    public Collection getPools() {
        Map response = this.piperRestAPIClient.getPools();

        assertNotTruncated(response);

        Collection data = (Collection)response.get("data");
        Collection result = new ArrayList<String>();
        Iterator<Object> iterator = data.iterator();
        while (iterator.hasNext()) {
            Map pool = (Map)iterator.next();
            result.add(pool.get("pool_id"));
        }
        return result;
    }

    private void assertNotTruncated(Map response) {
        Integer results = (Integer)response.get(TOTAL_RESULTS);
        Integer pageSize = (Integer)response.get(PAGE_SIZE);
        if (results > pageSize) {
            throw new IllegalArgumentException("results truncated");
        }
    }

}
