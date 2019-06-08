package com.hortonworks.streamline.streams.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hortonworks.streamline.streams.catalog.Topology;

import java.util.Collection;

public class TopologiesWithCountDto {
    @JsonProperty
    Collection<Topology> topologies;

    @JsonProperty
    int totalRecords;

    public Collection<Topology> getTopologies() {
        return topologies;
    }

    public void setTopologies(Collection<Topology> topologies) {
        this.topologies = topologies;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }
}
