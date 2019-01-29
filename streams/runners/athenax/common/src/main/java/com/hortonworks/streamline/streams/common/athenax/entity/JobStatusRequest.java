package com.hortonworks.streamline.streams.common.athenax.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobStatusRequest {
  @JsonProperty
  private String dataCenter;

  @JsonProperty
  private String cluster;

  @JsonProperty
  private String yarnApplicationId;

  public void setDataCenter(String dc) { this.dataCenter = dc; }
  public String getDataCenter() { return dataCenter; }

  public void setCluster(String cluster) { this.cluster = cluster; }
  public String getCluster() { return cluster; }

  public void setYarnApplicationId(String appId) { this.yarnApplicationId = appId; }
  public String getYarnApplicationId() { return yarnApplicationId; }
}