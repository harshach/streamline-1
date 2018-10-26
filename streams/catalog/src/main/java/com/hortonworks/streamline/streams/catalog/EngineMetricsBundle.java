package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.common.MetricsUISpec;
import com.hortonworks.streamline.common.MetricsUISpec.TimeseriesMetricField;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.annotation.StorableEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@StorableEntity
public class EngineMetricsBundle implements Storable {
	private static final Logger LOG = LoggerFactory.getLogger(EngineMetricsBundle.class);

    public static final String NAME_SPACE = "engine_metrics_bundle";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TIMESTAMP = "timestamp";
    public static final String ENGINE = "engine";
    public static final String TEMPLATE = "template";
    public static final String METRICS_SPEC = "metricsUISpec";

    /**
     * Unique id for a topology bundle component. This is the primary key
     */
    private Long id;

    /**
     * User assigned human readable name
     */
    private String name;


    /**
     * Time recording the creation or last update of this instance
     */
    private Long timestamp;

    /**
     * Underlying engine. For e.g. STORM. This is not an enum
     * because we want the user to be able to add new components without
     * changing code
     */
    private String engine;

    /**
     * Underlying template. For e.g. BLANK.
     */
    private String[] template;


    /**
     * Object that will be used by ui to render metrics coming from runners
     */
    private MetricsUISpec metricsUISpec;


    @Override
    @JsonIgnore
    public String getNameSpace () {
        return NAME_SPACE;
    }

    @Override
    @JsonIgnore
    public Schema getSchema () {
        return Schema.of(
                new Schema.Field(ID, Schema.Type.LONG),
                new Schema.Field(NAME, Schema.Type.STRING),
                new Schema.Field(TIMESTAMP, Schema.Type.LONG),
                new Schema.Field(ENGINE, Schema.Type.STRING),
                new Schema.Field(TEMPLATE, Schema.Type.ARRAY),
                new Schema.Field(METRICS_SPEC, Schema.Type.STRING)
        );
    }

    @Override
    @JsonIgnore
    public PrimaryKey getPrimaryKey () {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG),
                this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    @JsonIgnore
    public StorableKey getStorableKey () {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @Override
    public Map toMap () {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new HashMap<String, Object>();
        String metricsUISpecStr;
        try {
            metricsUISpecStr = mapper.writeValueAsString(metricsUISpec);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        map.put(ID, id);
        map.put(NAME, name);
        map.put(TIMESTAMP, timestamp);
        map.put(ENGINE, engine);
        map.put(TEMPLATE, String.join(",", template));
        map.put(METRICS_SPEC, metricsUISpecStr);
        return map;
    }

    @Override
    public Storable fromMap (Map<String, Object> map) {
        id = (Long) map.get(ID);
        name = (String)  map.get(NAME);
        timestamp = (Long) map.get(TIMESTAMP);
        engine = (String) map.get(ENGINE);
        template = ((String)map.get(TEMPLATE)).split(",");
        ObjectMapper mapper = new ObjectMapper();
        try {
        	metricsUISpec = mapper.readValue((String) map.get(METRICS_SPEC), MetricsUISpec.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Long getId () {
        return id;
    }

    public void setId (Long id) {
        this.id = id;
    }

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public Long getTimestamp () {
        return timestamp;
    }

    public void setTimestamp (Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String[] getTemplate() {
        return template.clone();
    }

    public void setTemplate(String[] template) {
        this.template = template.clone();
    }

    public MetricsUISpec getMetricsUISpec() {
        return metricsUISpec;
    }

    public void setMetricsUISpec (MetricsUISpec metricsUISpec) {
        this.metricsUISpec = metricsUISpec;
    }

    @Override
    public String toString () {
        return "EngineMetricsBundle{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", timestamp=" + timestamp +
                ", engine='" + engine + '\'' +
                ", template='" + Arrays.toString(template) + '\'' +
                ", metricsUISpec='" + metricsUISpec +
                '}';
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EngineMetricsBundle that = (EngineMetricsBundle) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (engine != null ? !engine.equals(that.engine) : that.engine != null)
            return false;
        if (template != null ? !Arrays.equals(template, that.template) : that.template != null)
            return false;
        if (metricsUISpec != null ? !metricsUISpec.equals(that.metricsUISpec) : that.metricsUISpec!= null)
            return false;
        return true;
    }

    @Override
    public int hashCode () {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (engine != null ? engine.hashCode() : 0);
        result = 31 * result + (template != null ? Arrays.hashCode(template) : 0);
        result = 31 * result + (metricsUISpec != null ? metricsUISpec.hashCode() : 0);
        return result;
    }

}
