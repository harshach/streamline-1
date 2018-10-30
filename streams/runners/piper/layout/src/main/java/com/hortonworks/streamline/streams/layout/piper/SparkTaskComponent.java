package com.hortonworks.streamline.streams.layout.piper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.piper.common.pipeline.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Spark Task Component (used both for PySpark and Spark).
 */
public class SparkTaskComponent extends GenericPiperTaskComponent {

    private static final Logger LOG = LoggerFactory.getLogger(SparkTaskComponent.class);

    protected static final String TASK_SPARK_ARGS = TASK_ARGS_PREFIX + '.' + "spark_args";
    protected static final String TASK_SPARK_OPTS = TASK_ARGS_PREFIX + '.' + "spark_opts";
    protected static final String TASK_SPARK_ENV_ADDITIONS = TASK_ARGS_PREFIX + '.' + "env_additions";
    protected static final String TASK_SPARK_USE_DROGON = "use_drogon";
    protected static final String SPARK_COMPUTE_SERVICE_ENV = "spark_compute_service";
    protected static final String SPARK_OPTS_KEY_PREFIX = "--";

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Task generateTask() {

        Task task = super.generateTask();
        setSparkOpts(task);
        setSparkArgs(task);
        setSparkEnvAdditions(task);
        setDrogonOption(task);
        return task;
    }

    private void setSparkArgs(Task task) {
        if (config.containsKey(TASK_SPARK_ARGS)) {
            List sparkArgs = new ArrayList<String>();
            Map kv = parseKeyValue((String)config.get(TASK_SPARK_ARGS));
            for (Object e: kv.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>)e;
                if (!entry.getKey().isEmpty()) {
                    sparkArgs.add(entry.getKey());
                }
                if (!entry.getValue().isEmpty()) {
                    sparkArgs.add(entry.getValue());
                }
            }
            task.getTaskParams().setParams(stripPrefix(TASK_SPARK_ARGS), sparkArgs);
        }
    }

    private void setSparkOpts(Task task) {
        if (config.containsKey(TASK_SPARK_OPTS)) {
            Map sparkOpts = new HashMap<String, String>();
            Map kv = parseKeyValue((String)config.get(TASK_SPARK_OPTS));
            for (Object e: kv.entrySet()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) e;
                if (!entry.getKey().isEmpty() && !entry.getValue().isEmpty()) {
                    String key = entry.getKey();
                    if (!key.startsWith(SPARK_OPTS_KEY_PREFIX)) {
                        key = SPARK_OPTS_KEY_PREFIX + key;
                    }
                    sparkOpts.put(key, entry.getValue());
                }
            }
            task.getTaskParams().setParams(stripPrefix(TASK_SPARK_OPTS), sparkOpts);
        }
    }

    private void setSparkEnvAdditions(Task task) {
        if (config.containsKey(TASK_SPARK_ENV_ADDITIONS)) {
            Map kv = parseKeyValue((String)config.get(TASK_SPARK_ENV_ADDITIONS));
            task.getTaskParams().setParams(stripPrefix(TASK_SPARK_ENV_ADDITIONS), kv);
        } else {
            task.getTaskParams().setParams(stripPrefix(TASK_SPARK_ENV_ADDITIONS), new HashMap());
        }
    }

    private void setDrogonOption(Task task) {
        if (config.containsKey(TASK_SPARK_USE_DROGON)) {
            if ((Boolean)config.get(TASK_SPARK_USE_DROGON)) {
                Map envAdditions = (HashMap<String, String>)task.getTaskParams().getParams().get(stripPrefix(TASK_SPARK_ENV_ADDITIONS));
                envAdditions.put(SPARK_COMPUTE_SERVICE_ENV, true);
            }
        }
    }

    private Map<String,String> parseKeyValue(String json) {
        TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String,String>>() {};
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (IOException e) {
            LOG.error("Error parsing JSON", e);
        }
        return new HashMap();
    }

}
