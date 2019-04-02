package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.annotation.SearchableField;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;

@StorableEntity
public class Template implements Storable {
    public static final String NAMESPACE = "template";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String ENGINEID = "engineId";
    public static final String TOPOLOGY_ACTION_CLASS = "topologyActionClass";
    public static final String TOPOLOGY_STATE_MACHINE_CLASS = "topologyStateMachineClass";
    public static final String TOPOLOGY_STATUS_METRICS_CLASS = "topologyStatusMetricsClass";
    public static final String TOPOLOGY_TIME_SERIES_METRICS_CLASS = "topologyTimeSeriesMetricsClass";
    public static final String CONFIG = "config";


    private Long id;

    @SearchableField
    private String name;

    @SearchableField
    private String descrption;

    private Long engineId;

    private String topologyActionClass;

    private String topologyStateMachineClass;

    private String topologyStatusMetricsClass;

    private String topologyTimeseriesMetricsClass;

    private String config;

    public Template() {}

    public Template(Template other) {
        if (other != null) {
            setId(other.getId());
            setName(other.getName());
            setDescription(other.getDescription());
            setEngineId(other.getEngineId());
            setTopologyActionClass(other.getTopologyActionClass());
            setTopologyStateMachineClass(other.getTopologyStateMachineClass());
            setTopologyStatusMetricsClass(other.getTopologyStatusMetricsClass());
            setTopologyTimeseriesMetricsClass(other.getTopologyTimeseriesMetricsClass());
            setConfig(other.getConfig());
        }
    }


    @JsonIgnore
    public String getNameSpace () {
        return NAMESPACE;
    }

    @JsonIgnore
    public Schema getSchema () {
        return Schema.of(
                new Schema.Field(ID, Schema.Type.LONG),
                new Schema.Field(NAME, Schema.Type.STRING),
                new Schema.Field(DESCRIPTION, Schema.Type.STRING),
                new Schema.Field(ENGINEID, Schema.Type.LONG),
                new Schema.Field(TOPOLOGY_ACTION_CLASS, Schema.Type.STRING),
                new Schema.Field(TOPOLOGY_STATE_MACHINE_CLASS, Schema.Type.STRING),
                new Schema.Field(TOPOLOGY_STATUS_METRICS_CLASS, Schema.Type.STRING),
                new Schema.Field(TOPOLOGY_TIME_SERIES_METRICS_CLASS, Schema.Type.STRING),
                new Schema.Field(CONFIG, Schema.Type.STRING)
        );
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey () {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    public StorableKey getStorableKey () {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    public Map toMap () {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, this.id);
        map.put(NAME, this.name);
        map.put(DESCRIPTION, this.descrption);
        map.put(ENGINEID, this.engineId);
        map.put(TOPOLOGY_ACTION_CLASS, this.topologyActionClass);
        map.put(TOPOLOGY_STATE_MACHINE_CLASS, this.topologyStateMachineClass);
        map.put(TOPOLOGY_STATUS_METRICS_CLASS, this.topologyStatusMetricsClass);
        map.put(TOPOLOGY_TIME_SERIES_METRICS_CLASS, this.topologyTimeseriesMetricsClass);
        map.put(CONFIG, this.config);
        return map;
    }

    public Template fromMap (Map<String, Object> map) {
        this.id = (Long) map.get(ID);
        this.name = (String) map.get(NAME);
        this.descrption = (String) map.get(DESCRIPTION);
        this.engineId = (Long) map.get(ENGINEID);
        this.topologyActionClass = (String) map.get(TOPOLOGY_ACTION_CLASS);
        this.topologyStateMachineClass = (String) map.get(TOPOLOGY_STATE_MACHINE_CLASS);
        this.topologyStatusMetricsClass = (String) map.get(TOPOLOGY_STATUS_METRICS_CLASS);
        this.topologyTimeseriesMetricsClass = (String) map.get(TOPOLOGY_TIME_SERIES_METRICS_CLASS);
        this.config = (String)  map.get(CONFIG);
        return this;
    }

    public Long getId() {return id;}

    public void setId(Long id) {this.id = id;}

    public String getName() {return name;}

    public void setName(String name) { this.name = name; }

    public String getDescription() { return descrption; }

    public void setDescription(String description) { this.descrption = description; }

    public Long getEngineId() { return engineId; }

    public void setEngineId(Long engineId) { this.engineId = engineId; }

    public String getTopologyActionClass() { return topologyActionClass; }

    public void setTopologyActionClass(String topologyActionClass) { this.topologyActionClass = topologyActionClass; }

    public String getTopologyStateMachineClass() { return topologyStateMachineClass; }

    public void setTopologyStateMachineClass(String topologyStateMachineClass) { this.topologyStateMachineClass = topologyStateMachineClass; }

    public String getTopologyTimeseriesMetricsClass() { return topologyTimeseriesMetricsClass; }

    public void setTopologyTimeseriesMetricsClass(String topologyTimeseriesMetricsClass) {
        this.topologyTimeseriesMetricsClass = topologyTimeseriesMetricsClass;
    }

    public String getTopologyStatusMetricsClass() { return topologyStatusMetricsClass; }

    public void setTopologyStatusMetricsClass(String topologyStatusMetricsClass ) { this.topologyStatusMetricsClass = topologyStatusMetricsClass; }

    public String getConfig() { return config; }

    public void setConfig(String config) { this.config = config; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Template template = (Template) o;

        if (id != null ? !id.equals(template.id) : template.id != null) return false;
        if (name != null ? !name.equals(template.getName()): template.getName() != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }


}
