/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/


package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

@StorableEntity
public class TopologyRuntimeIdMap extends AbstractStorable {
    public static final String NAMESPACE = "topology_runtime_id_map";
    public static final String FIELD_TOPOLOGY_ID = "topologyId";
    public static final String FIELD_NAMESPACE_ID = "namespaceId";
    public static final String FIELD_APPLICATION_ID = "applicationId";

    private Long topologyId;
    private Long namespaceId;
    private String applicationId;

    public TopologyRuntimeIdMap() {}

    public TopologyRuntimeIdMap(Long topologyId, Long namespaceId, String applicationId) {
        this.topologyId = topologyId;
        this.namespaceId = namespaceId;
        this.applicationId = applicationId;
    }

    @JsonIgnore
    @Override
    public String getNameSpace() { return NAMESPACE; }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(Schema.Field.of(FIELD_TOPOLOGY_ID, Schema.Type.LONG), this.topologyId);
        fieldToObjectMap.put(Schema.Field.of(FIELD_NAMESPACE_ID, Schema.Type.LONG), this.namespaceId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    public Long getTopologyId() { return topologyId; }

    public void setTopologyId(Long topologyId) { this.topologyId = topologyId; }

    @JsonIgnore
    public Long getNamespaceId() { return namespaceId; }

    public void setNamespaceId(Long namespaceId) { this.namespaceId = namespaceId; }

    @JsonProperty("applicationId")
    public String getApplicationId() { return applicationId; }

    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyRuntimeIdMap that = (TopologyRuntimeIdMap) o;

        if (topologyId != null ? !topologyId.equals(that.topologyId) : that.topologyId != null) return false;
        if (namespaceId != null ? !namespaceId.equals(that.namespaceId) : that.namespaceId != null) return false;
        return applicationId != null ? applicationId.equals(that.applicationId) : that.applicationId == null;

    }

    @Override
    public int hashCode() {
        int result = topologyId != null ? topologyId.hashCode() : 0;
        result = 31 * result + (namespaceId != null ? namespaceId.hashCode() : 0);
        result = 31 * result + (applicationId != null ? applicationId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TopologyRuntimeIdMap{" +
                "topologyId=" + topologyId +
                ", namespaceId=" + namespaceId +
                ", applicationId=" + applicationId +
                "} " + super.toString();
    }

}
