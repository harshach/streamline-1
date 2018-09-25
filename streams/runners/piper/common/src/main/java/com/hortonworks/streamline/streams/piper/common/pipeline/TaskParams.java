package com.hortonworks.streamline.streams.piper.common.pipeline;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskParams {

    @JsonIgnore
    private Map<String, Object> params = new HashMap<String, Object>();

    @JsonAnyGetter
    public Map<String, Object> getParams() {
        return this.params;
    }

    @JsonAnySetter
    public void setParams(String name, Object value) {
        this.params.put(name, value);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TaskParams) == false) {
            return false;
        }
        return new EqualsBuilder().isEquals();
    }

}
