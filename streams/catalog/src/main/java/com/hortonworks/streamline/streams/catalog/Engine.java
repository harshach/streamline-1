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
public class Engine implements Storable {
    public static final String NAMESPACE = "engine";
    public static final String ID =  "id";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String DISPLAYNAME = "displayName";
    public static final String DEPLOYMENTMODES = "deploymentModes";
    public static final String COMPONENTTYPES = "componentTypes";
    public static final String SCHEMAAWARE = "schemaAware";
    public static final String ENABLED = "enabled";
    public static final String CONFIG = "config";

    private Long id;

    @SearchableField
    private String name;

    private String type;

    @SearchableField
    private String displayName;

    private String deploymentModes;

    private String componentTypes;

    private boolean schemaAware;

    private boolean enabled;

    private String config;

    public Engine() {}

    public Engine(Engine other) {
        if (other != null) {
            setId(other.getId());
            setName(other.getName());
            setType(other.getType());
            setDisplayName(other.getDisplayName());
            setDeploymentModes(other.getDeploymentModes());
            setComponentTypes(other.getComponentTypes());
            setSchemaAware(other.getSchemaAware());
            setEnabled(other.getEnabled());
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
                new Schema.Field(TYPE, Schema.Type.STRING),
                new Schema.Field(DISPLAYNAME, Schema.Type.STRING),
                new Schema.Field(DEPLOYMENTMODES, Schema.Type.STRING),
                new Schema.Field(COMPONENTTYPES, Schema.Type.STRING),
                new Schema.Field(SCHEMAAWARE, Schema.Type.BOOLEAN),
                new Schema.Field(ENABLED, Schema.Type.BOOLEAN),
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
        map.put(TYPE, this.type);
        map.put(DISPLAYNAME, this.displayName);
        map.put(DEPLOYMENTMODES, this.deploymentModes);
        map.put(COMPONENTTYPES, this.componentTypes);
        map.put(SCHEMAAWARE, this.schemaAware);
        map.put(ENABLED, this.enabled);
        map.put(CONFIG, this.config);
        return map;
    }

    public Engine fromMap (Map<String, Object> map) {
        this.id = (Long) map.get(ID);
        this.name = (String) map.get(NAME);
        this.type = (String) map.get(TYPE);
        this.displayName = (String) map.get(DISPLAYNAME);
        this.deploymentModes = (String) map.get(DEPLOYMENTMODES);
        this.componentTypes = (String) map.get(COMPONENTTYPES);
        this.schemaAware = (boolean) map.get(SCHEMAAWARE);
        this.enabled = (boolean) map.get(ENABLED);
        this.config = (String)  map.get(CONFIG);
        return this;
    }

    public Long getId() {return id;}

    public void setId(Long id) {this.id = id;}

    public String getName() {return name;}

    public void setName(String name) { this.name = name; }

    public String getType() {return type;}

    public void setType(String type) { this.type = type; }

    public String getDisplayName() { return displayName; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDeploymentModes() { return deploymentModes; }

    public void setDeploymentModes(String deploymentModes) { this.deploymentModes = deploymentModes; }

    public String getComponentTypes() { return componentTypes; }

    public void setComponentTypes(String componentTypes) { this.componentTypes = componentTypes; }

    public boolean getSchemaAware() { return schemaAware; }

    public void setSchemaAware(boolean schemaAware) { this.schemaAware = schemaAware; }

    public boolean getEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getConfig() { return config; }

    public void setConfig(String config) { this.config = config; }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Engine engine = (Engine) o;

        if (id != null ? !id.equals(engine.id) : engine.id != null) return false;
        if (name != null ? !name.equals(engine.getName()): engine.getName() != null) return false;
        return type != null ? !type.equals(engine.getType()) : engine.getType() != null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 *  result + (type != null ? type.hashCode() : 0);
        return result;
    }

}
