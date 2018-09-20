package com.hortonworks.streamline.streams.piper.common.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_id")
    private String taskId;
    @JsonProperty("retries")
    private Double retries;
    @JsonProperty("retry_delay")
    private Double retryDelay;
    @JsonProperty("trigger_rule")
    private Task.TriggerRule triggerRule;
    @JsonProperty("execution_timeout")
    private Double executionTimeout;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pool")
    private String pool;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("dependencies")
    private List<String> dependencies = new ArrayList<String>();
    @JsonProperty("connections")
    private Connections connections;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_class")
    private String taskClass;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_params")
    private TaskParams taskParams;


    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_id")
    public String getTaskId() {
        return taskId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_id")
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @JsonProperty("retries")
    public Double getRetries() {
        return retries;
    }

    @JsonProperty("retries")
    public void setRetries(Double retries) {
        this.retries = retries;
    }

    @JsonProperty("retry_delay")
    public Double getRetryDelay() {
        return retryDelay;
    }

    @JsonProperty("retry_delay")
    public void setRetryDelay(Double retryDelay) {
        this.retryDelay = retryDelay;
    }

    @JsonProperty("trigger_rule")
    public Task.TriggerRule getTriggerRule() {
        return triggerRule;
    }

    @JsonProperty("trigger_rule")
    public void setTriggerRule(Task.TriggerRule triggerRule) {
        this.triggerRule = triggerRule;
    }

    @JsonProperty("execution_timeout")
    public Double getExecutionTimeout() {
        return executionTimeout;
    }

    @JsonProperty("execution_timeout")
    public void setExecutionTimeout(Double executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pool")
    public String getPool() {
        return pool;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("pool")
    public void setPool(String pool) {
        this.pool = pool;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("dependencies")
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("dependencies")
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    @JsonProperty("connections")
    public Connections getConnections() {
        return connections;
    }

    @JsonProperty("connections")
    public void setConnections(Connections connections) {
        this.connections = connections;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_class")
    public String getTaskClass() {
        return taskClass;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_class")
    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_params")
    public TaskParams getTaskParams() {
        return taskParams;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("task_params")
    public void setTaskParams(TaskParams taskParams) {
        this.taskParams = taskParams;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("taskId", taskId).append("retries", retries).append("retryDelay", retryDelay).append("triggerRule", triggerRule).append("executionTimeout", executionTimeout).append("pool", pool).append("dependencies", dependencies).append("connections", connections).append("taskClass", taskClass).append("taskParams", taskParams).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(retryDelay).append(retries).append(executionTimeout).append(pool).append(taskClass).append(triggerRule).append(taskParams).append(taskId).append(connections).append(dependencies).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Task) == false) {
            return false;
        }
        Task rhs = ((Task) other);
        return new EqualsBuilder().append(retryDelay, rhs.retryDelay).append(retries, rhs.retries).append(executionTimeout, rhs.executionTimeout).append(pool, rhs.pool).append(taskClass, rhs.taskClass).append(triggerRule, rhs.triggerRule).append(taskParams, rhs.taskParams).append(taskId, rhs.taskId).append(connections, rhs.connections).append(dependencies, rhs.dependencies).isEquals();
    }

    public enum TriggerRule {

        ALL_SUCCESS("all_success"),
        ALL_FAILED("all_failed"),
        ALL_DONE("all_done"),
        ONE_SUCCESS("one_success"),
        ONE_FAILED("one_failed"),
        DUMMY("dummy");
        private final String value;
        private final static Map<String, Task.TriggerRule> CONSTANTS = new HashMap<String, Task.TriggerRule>();

        static {
            for (Task.TriggerRule c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private TriggerRule(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Task.TriggerRule fromValue(String value) {
            Task.TriggerRule constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
