package com.hortonworks.streamline.streams.layout.piper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.piper.common.pipeline.Task;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Spark Task Component (used both for PySpark and Spark).
 */
public class SparkTaskComponent extends GenericPiperTaskComponent {
    private static final Logger LOG = LoggerFactory.getLogger(SparkTaskComponent.class);

    private static final String TASK_SPARK_ARGS = TASK_ARGS_PREFIX + '.' + "spark_args";
    private static final String TASK_SPARK_OPTS = TASK_ARGS_PREFIX + '.' + "spark_opts";
    private static final String TASK_SPARK_ENV_ADDITIONS = TASK_ARGS_PREFIX + '.' + "env_additions";
    private static final String TASK_SPARK_VERSION = TASK_ARGS_PREFIX + "." + "spark_version";
    private static final String SPARK_VERSION_ENV = "SPARK_VERSION";
    private static final String SPARK_OPTS_KEY_PREFIX = "--";
    private static final List<String> PREDEFINED_SPARK_OPTS = Arrays
        .asList("queue", "num_executors", "executor_cores", "executor_memory", "driver_cores",
            "driver_memory");

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Task generateTask() {

        Task task = super.generateTask();
        setPredefinedSparkOpts(task);
        setSparkOpts(task);
        setSparkArgs(task);
        setSparkEnvAdditions(task);
        setSparkVersion(task);
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
            Map sparkOpts = (Map)task.getTaskParams().getParams().get(stripPrefix(TASK_SPARK_OPTS));
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
        }
    }

    private void setPredefinedSparkOpts(Task task) {
        Map sparkOpts = new HashMap<String, Object>();
        for (String predefinedProperty : PREDEFINED_SPARK_OPTS) {
            Object value = config.get(TASK_SPARK_OPTS + "." + predefinedProperty);
            String predefinedPropertyWithUnderscore = predefinedProperty.replaceAll("_", "-");
            sparkOpts.put(SPARK_OPTS_KEY_PREFIX + predefinedPropertyWithUnderscore, value);
        }
        task.getTaskParams().setParams(stripPrefix(TASK_SPARK_OPTS), sparkOpts);
    }

    private void setSparkEnvAdditions(Task task) {
        if (config.containsKey(TASK_SPARK_ENV_ADDITIONS)) {
            Map kv = parseKeyValue((String)config.get(TASK_SPARK_ENV_ADDITIONS));
            task.getTaskParams().setParams(stripPrefix(TASK_SPARK_ENV_ADDITIONS), kv);
        } else {
            task.getTaskParams().setParams(stripPrefix(TASK_SPARK_ENV_ADDITIONS), new HashMap());
        }
    }

    private void setSparkVersion(Task task) {
        if (config.containsKey(TASK_SPARK_VERSION)) {
            Map envAdditions = (HashMap<String, String>) task.getTaskParams().getParams()
                .get(stripPrefix(TASK_SPARK_ENV_ADDITIONS));
            envAdditions.put(SPARK_VERSION_ENV, config.get(TASK_SPARK_VERSION));
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
