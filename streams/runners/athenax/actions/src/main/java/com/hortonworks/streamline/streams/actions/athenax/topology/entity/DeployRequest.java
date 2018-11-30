package com.hortonworks.streamline.streams.actions.athenax.topology.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployRequest {
  private static final int DEFAULT_YARN_CONTAINER_COUNT = 2;
  private static final int DEFAULT_YARN_CONTAINER_MEM = 2048;
  private static final int DEFAULT_SLOT_PER_YARN_CONTAINER = 1;
  private static final boolean DEFAULT_BACKFILL_VALUE = false;
  private static final String STREAM_JOB_TYPE = "Stream";
  private static final String BATCH_JOB_TYPE = "Batch";

  @JsonProperty
  private JobDefinition jobDefinition;
  @JsonProperty
  private int yarnContainerCount = DEFAULT_YARN_CONTAINER_COUNT;
  @JsonProperty
  private int yarnMemoryPerContainerInMB = DEFAULT_YARN_CONTAINER_MEM;
  @JsonProperty
  private int slotPerYarnContainer = DEFAULT_SLOT_PER_YARN_CONTAINER;
  @JsonProperty
  private String dataCenter;
  @JsonProperty
  private String cluster;
  @JsonProperty
  private Boolean isBackfill = DEFAULT_BACKFILL_VALUE;
  @JsonProperty
  private String jobType;

  public void setJobDefinition(JobDefinition jobDefinition) {
    this.jobDefinition = jobDefinition;
  }

  public void setYarnContainerCount(int yarnContainerCount) {
    this.yarnContainerCount = yarnContainerCount;
  }

  public void setSlotPerYarnContainer(int slotPerYarnContainer) {
    this.slotPerYarnContainer = slotPerYarnContainer;
  }

  public void setYarnMemoryPerContainerInMB(int yarnMemoryPerContainerInMB) {
    this.yarnMemoryPerContainerInMB = yarnMemoryPerContainerInMB;
  }

  public void setDataCenter(String dataCenter) {
    this.dataCenter = dataCenter;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public void setBackfill(Boolean backfill) {
    isBackfill = backfill;
    jobType = isBackfill ? BATCH_JOB_TYPE : STREAM_JOB_TYPE;
  }

  public JobDefinition jobDefinition() {
    return jobDefinition;
  }

  public int yarnContainerCount() {
    return yarnContainerCount;
  }

  public int slotPerYarnContainer() {
    return slotPerYarnContainer;
  }

  public int yarnMemoryPerContainerInMB() {
    return yarnMemoryPerContainerInMB;
  }

  public String dataCenter() {
    return dataCenter;
  }

  public String cluster() {
    return cluster;
  }

  public Boolean isBackfill() {
    return isBackfill;
  }

  public String getJobType() {
    return jobType;
  }
}
