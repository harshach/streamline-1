package com.hortonworks.streamline.streams.layout.piper;

import com.hortonworks.streamline.streams.piper.common.pipeline.Task;
import com.hortonworks.streamline.streams.piper.common.pipeline.TaskParams;

import java.util.Map;

/**
 * Generic task component.
 * Acts as passthrough to managed Pipeline API.
 * Expects class_name and task_params to be supplied as part of fields.
 *
 */
public class GenericPiperTaskComponent extends AbstractPiperTaskComponent {

    public static final String TASK_ARGS_PREFIX = "task_params";
    public static final String TASK_CLASS_NAME = "class_name";

    @Override
    public Task generateTask() {
        Task task = createTask();
        setTaskParams(task);
        setTaskClass(task);
        return task;
    }

    /**
     * Set Task parameters automatically.
     * Will expect task key with prefix `task_params.[key]`
     * @param task
     */
    protected void setTaskParams(Task task) {
        TaskParams taskParams = new TaskParams();
        for (Map.Entry<String, Object> entry: config.entrySet()) {
            if (entry.getKey().startsWith(TASK_ARGS_PREFIX)) {
                String key = entry.getKey();
                taskParams.setParams(stripPrefix(key), entry.getValue());
            }
        }
        task.setTaskParams(taskParams);
    }

    protected String stripPrefix(String fullKey) {
        return fullKey.replaceFirst(TASK_ARGS_PREFIX + ".", "");
    }

    /**
     * Set task class
     * @return
     */
    protected void setTaskClass(Task task) {
        task.setTaskClass((String)config.get(TASK_CLASS_NAME));
    }
}
