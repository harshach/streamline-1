package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.exception.NoTopologyDeploymentConfigException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CatalogToDeploymentConverter {
    private static final String DEPLOYMENT_SETTINGS = "deploymentSettings";
    private static final String DEPLOYMENT_MODE = "deploymentMode";
    private static final String NAMESPACE_IDS = "namespaceIds";

    private CatalogToDeploymentConverter() {
    }

    public static TopologyDeployment getTopologyDeployment(Topology topology) throws NoTopologyDeploymentConfigException {
      try {
          if (!StringUtils.isEmpty(topology.getConfig())) {
              ObjectMapper mapper = new ObjectMapper();
              Map<String, Object> config = mapper.readValue(topology.getConfig(), new TypeReference<Map<String, Object>>() {
              });
              Map<String, Object> deploymentConfig = (Map<String, Object>) config.get(DEPLOYMENT_SETTINGS);
              TopologyDeployment.DeploymentSetting deploymentSetting;
              if (deploymentConfig.get(DEPLOYMENT_MODE).equals("ALL")) {
                  deploymentSetting = TopologyDeployment.DeploymentSetting.ALL_REGIONS;
              } else {
                  deploymentSetting = TopologyDeployment.DeploymentSetting.CHOSEN_REGION;
              }
              List<Long> regions = (ArrayList<Long>) deploymentConfig.get(NAMESPACE_IDS);
              return new TopologyDeployment(deploymentSetting, regions);
          }

        } catch (IOException e) {
              throw new NoTopologyDeploymentConfigException("There is no deployment config found for topology %s".format(topology.getName()));
        }
        return null;
    }

}
