package com.hortonworks.streamline.streams.actions.athenax.topology.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RTADeployTableRequest {
    @JsonProperty
    private String kafkaClusterName;

    public String kafkaClusterName() {
        return kafkaClusterName;
    }

    public void setKafkaCluster(String kafkaClusterName) {
        this.kafkaClusterName = kafkaClusterName;
    }
}
