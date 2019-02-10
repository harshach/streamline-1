package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.exception.NoTopologyDeploymentConfigException;
import com.hortonworks.streamline.common.Config;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public final class CatalogToDeploymentConverter {
    private static final String DEPLOYMENT_MODE = "topology.deploymentMode";
    private static final String NAMESPACE_IDS = "topology.namespaceIds";

    private CatalogToDeploymentConverter() {
    }

    public static TopologyDeployment getTopologyDeployment(Topology topology) throws NoTopologyDeploymentConfigException {
      Config config = topology.getConfig();
      if (config != null) {
          Map<String, Object> topologyConfig =  config.getProperties();
          TopologyDeployment.DeploymentSetting deploymentSetting;
          String deploymentMode = (String) topologyConfig.get(DEPLOYMENT_MODE);
          if (deploymentMode.equals("ALL")) {
              deploymentSetting = TopologyDeployment.DeploymentSetting.ALL_REGIONS;
          } else {
              deploymentSetting = TopologyDeployment.DeploymentSetting.CHOSEN_REGION;
          }

          List<Long> regions;
          String namespaceIdStr = (String) topologyConfig.get(NAMESPACE_IDS);
          if (!StringUtils.isEmpty(namespaceIdStr)) {
              try {
                  ObjectMapper mapper = new ObjectMapper();
                  regions = mapper.readValue(namespaceIdStr, new TypeReference<List<Long>>() {
                  });
                  return new TopologyDeployment(deploymentSetting, regions);
              } catch(IOException e) {
                  throw new NoTopologyDeploymentConfigException("Failed to parse topology deployment settings for %s".format(topology.getName()));
              }
          }
      }
       throw new NoTopologyDeploymentConfigException("Failed to parse topology deployment settings for %s".format(topology.getName()));
    }

}
