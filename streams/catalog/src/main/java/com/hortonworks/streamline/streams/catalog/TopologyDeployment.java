package com.hortonworks.streamline.streams.catalog;


import java.util.List;

public class TopologyDeployment {
    private DeploymentSetting deploymentSetting;
    private List<Long> regions;

    public TopologyDeployment(DeploymentSetting deploymentSetting, List<Long> regions) {
        this.deploymentSetting = deploymentSetting;
        this.regions = regions;
    }

    public DeploymentSetting getDeploymentSetting() {
        return deploymentSetting;
    }

    public List<Long> getRegions() {
        return regions;
    }

    enum DeploymentSetting {
        ALL_REGIONS, CHOSEN_REGION
    }
}
