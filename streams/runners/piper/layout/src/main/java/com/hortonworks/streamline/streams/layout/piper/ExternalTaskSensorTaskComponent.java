package com.hortonworks.streamline.streams.layout.piper;

import com.hortonworks.streamline.streams.piper.common.pipeline.Task;

/**
 * ExternalTaskSensor component.
 */
public class ExternalTaskSensorTaskComponent extends GenericPiperTaskComponent {
    private static final String DELTA_MINUTES = "execution_delta.minutes";
    private static final String ALLOWED_STATES = "allowed_states";
    private static final String MATCH_SUCCESS_STATE = "success";
    private static final String SENSOR_OPT_EXECUTION_DELTA = "execution_delta";
    private static final String SENSOR_OPT_ALLOWED_STATES = "allowed_states";
    private static final String SENSOR_OPT_FAILED_STATES[] = new String[] {"failed", "shutdown", "upstream_failed", "skipped"};
    private static final String SENSOR_OPT_SUCCESS_STATES[] = new String[] {"success"};

    @Override
    public Task generateTask() {
        Task task = createTask();
        setTaskParams(task);
        setTaskClass(task);
        setSensorDelta(task);
        setSensorStates(task);
        return task;
    }

    private void setSensorStates(Task task) {
        if (config.containsKey(ALLOWED_STATES)) {
            if (config.get(ALLOWED_STATES).toString().toLowerCase().contains(MATCH_SUCCESS_STATE)) {
                task.getTaskParams().setParams(SENSOR_OPT_ALLOWED_STATES, SENSOR_OPT_SUCCESS_STATES);
            } else {
                task.getTaskParams().setParams(SENSOR_OPT_ALLOWED_STATES, SENSOR_OPT_FAILED_STATES);
            }
        }
    }

    private void setSensorDelta(Task task) {
        int delta = 0;
        if (config.containsKey(DELTA_MINUTES)) {
            delta += (Integer)config.get(DELTA_MINUTES);
        }

        delta = Math.abs(delta) * -1;
        task.getTaskParams().setParams(SENSOR_OPT_EXECUTION_DELTA, delta);
    }
}
