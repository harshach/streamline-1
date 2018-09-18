package com.hortonworks.streamline.streams.actions.athenax.topology.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDefinition {
  @JsonProperty
  private String jobName;

  @JsonProperty
  private String groupName;

  @JsonProperty
  private String serviceName;

  @JsonProperty
  private String transformQuery;

  @JsonProperty
  private List<Connector> input;

  @JsonProperty
  private List<Connector> output;

  @JsonProperty
  private boolean isBackfill = false;

  public String jobName() {
    return jobName;
  }

  public void setJobName(String jobName) { this.jobName = jobName; }

  public String transformQuery() {
    return transformQuery;
  }

  public void setTransformQuery(String query) {
    this.transformQuery = query;
  }

  /**
   * Group Name is used for security purpose.
   */
  public String groupName() {
    return groupName;
  }

  public void setGroupName(String groupName) { this.groupName = groupName; }

  /**
   * Service Name is used for uBlame / PagerDuty purpose.
   */
  public String serviceName() {
    return serviceName;
  }
  public void setServiceName(String serviceName) { this.serviceName = serviceName; }

  public List<Connector> input() {
    return input;
  }
  public void setInput(List<Connector> input) { this.input = input; }

  public List<Connector> output() {
    return output;
  }
  public void setOutput(List<Connector> connectors) {
    this.output = connectors;
  }

  public boolean isBackfill() {
    return isBackfill;
  }
  public void setIsBackfill(boolean isBackfill) { this.isBackfill = isBackfill; }

}