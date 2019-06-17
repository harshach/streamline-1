package com.hortonworks.streamline.streams.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hortonworks.streamline.streams.catalog.Topology;

import java.util.Collection;

public class TopologiesPaginationDto {
    @JsonProperty
    Collection<Topology> topologies;

    public Collection<Topology> getTopologies() {
        return topologies;
    }

    public void setTopologies(Collection<Topology> topologies) {
        this.topologies = topologies;
    }
}
