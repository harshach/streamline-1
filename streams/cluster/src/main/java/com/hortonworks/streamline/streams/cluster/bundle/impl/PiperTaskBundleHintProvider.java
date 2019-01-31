package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.hortonworks.streamline.streams.cluster.bundle.AbstractBundleHintProvider;
import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.cluster.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.service.metadata.PiperMetadataService;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PiperTaskBundleHintProvider extends AbstractBundleHintProvider {
    // Should match pool field in task component bundles
    public static final String POOL_FIELD_NAME = "pool";

    public Map<String, Object> getHintsOnCluster(Cluster cluster, SecurityContext securityContext, Subject subject) {
        Map<String, Object> hintMap = new HashMap<>();

        try {
            PiperMetadataService piperMetadataService = new PiperMetadataService(environmentService, cluster, securityContext, subject);

            if (getConnectionType() != null) {
                hintMap.put(getConnectionFieldName(), piperMetadataService.getConnections(getConnectionType()));
            }

            hintMap.put(POOL_FIELD_NAME, piperMetadataService.getPools());

        } catch (ServiceNotFoundException e) {
            throw new IllegalStateException(PiperMetadataService.PIPER_SERVICE_NAME + " Service in cluster " + cluster.getName() +
                    " not found but mapping information exists.");
        } catch (ServiceConfigurationNotFoundException e) {
            throw new IllegalStateException(PiperMetadataService.PIPER_SERVICE_NAME + "Service Configuration in cluster " + cluster.getName() +
                    " not found.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return hintMap;
    }

    public String getServiceName() {
        return "PIPER";
    }

    // Return null to prevent fetch
    public String getConnectionType() { return null; }

    public String getConnectionFieldName() { return null; }
}
