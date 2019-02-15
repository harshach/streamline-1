package com.hortonworks.streamline.streams.layout.piper;

import com.hortonworks.streamline.streams.piper.common.pipeline.Pipeline;
import com.hortonworks.streamline.streams.piper.common.pipeline.Task;

/**
 * Hive Task Component used for Hive Task.
 */
public class HiveTaskComponent extends GenericPiperTaskComponent {
    private static final String RUN_AS_OWNER = "run_as_owner";
    private static final boolean RUN_AS_OWNER_TRUE = true;

    private static final String HIVE_CLI_CONN_ID_KEY = "hive_cli_conn_id";
    private static final String HIVE_CLI_CONN_SECURE  = "hive_cli_default_secure";

    @Override
    public Task generateTask() {
        Task task = createTask();
        setTaskParams(task);
        setTaskClass(task);
        setRunAsOwner(task);
        setConnectionId(task);
        return task;
    }

    private void setRunAsOwner(Task task) {
        task.getTaskParams().setParams(RUN_AS_OWNER, RUN_AS_OWNER_TRUE);
    }

    private void setConnectionId(Task task) {
        if (this.pipeline != null && this.pipeline.getSecure()) {
            task.getTaskParams().setParams(HIVE_CLI_CONN_ID_KEY, HIVE_CLI_CONN_SECURE);
        }
    }
}
