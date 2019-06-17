package com.hortonworks.streamline.streams.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TopologiesWithCountDto extends TopologiesPaginationDto {
    @JsonProperty
    int totalRecords;

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getTotalRecords() {
        return totalRecords;
    }
}
