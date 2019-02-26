package com.hortonworks.streamline.streams.layout.piper;

import com.hortonworks.streamline.streams.layout.component.StreamlineTask;
import com.hortonworks.streamline.streams.piper.common.pipeline.Task;
import com.hortonworks.streamline.streams.piper.common.pipeline.Pipeline;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract component for constructing Piper DAG
 */
public abstract class AbstractPiperTaskComponent extends StreamlineTask implements PiperTaskComponent {

    public static final String TASK_POOL = "pool";
    public static final String TASK_RETRIES = "retries";
    public static final String TASK_RETRY_DELAY = "retry_delay";
    public static final String TASK_EXECUTION_TIMEOUT = "execution_timeout";
    public static final String TASK_TRIGGER_RULE = "trigger_rule";

    protected Map<String, Object> config = new HashMap();
    protected Pipeline pipeline = null;

    @Override
    public void withConfig(Map<String, Object> config, Pipeline pipeline) {
        this.config = config;
        this.pipeline = pipeline;
    }

    /**
     * Generate task based on component logic
     * @return
     */
    @Override
    public abstract Task generateTask();

    /**
     * Create task with common task properties
     * @return
     */
    protected Task createTask() {
        Task task = new Task();
        setCommonTaskProperties(task);
        return task;
    }

    /**
     * Set common task properties that all Piper tasks contain.
     * @return
     */
    protected void setCommonTaskProperties(Task task) {
        task.setPool((String)config.get(TASK_POOL));
        if (config.containsKey(TASK_EXECUTION_TIMEOUT)) {
            task.setExecutionTimeout((Integer)config.get(TASK_EXECUTION_TIMEOUT));
        }
        if (config.containsKey(TASK_RETRIES)) {
            task.setRetries((Integer)config.get(TASK_RETRIES));
        }
        if (config.containsKey(TASK_RETRY_DELAY)) {
            task.setRetryDelay((Integer)config.get(TASK_RETRY_DELAY));
        }
        if (config.containsKey(TASK_TRIGGER_RULE)) {
            task.setTriggerRule(Task.TriggerRule.fromValue((String) config.get(TASK_TRIGGER_RULE)));
        }
    }
}
