package com.hortonworks.streamline.streams.layout.piper.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.layout.piper.GenericPiperTaskComponent;
import com.hortonworks.streamline.streams.piper.common.pipeline.Task;
import com.hortonworks.streamline.streams.piper.common.pipeline.TaskParams;
import com.hortonworks.streamline.streams.piper.common.pipeline.contract.Destination;
import com.hortonworks.streamline.streams.piper.common.pipeline.contract.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPiperContractTaskComponent extends GenericPiperTaskComponent {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPiperContractTaskComponent.class);

    private static final String SOURCE_TASK_PARAM = "source";
    private static final String DESTINATION_TASK_PARAM = "destination";
    private static final String TEMPLATE_PARAMS = "template_params";
    private static final String SCRIPT_TYPE = "scriptType";
    private static final String SCRIPT_TYPE_SQL_OPTION = "scriptTypeSqlOption";
    private static final String SQL = "sql";
    private static final String SCRIPT_TYPE_QUERYBUILDER_REPORT_ID_OPTION =  "scriptTypeQuerybuilderReportIdOption";
    private static final String SCRIPT_TYPE_QUERYBUILDER_ID_OPTION = "scriptTypeQuerybuilderIdOption";
    private static final String QUERY_BUILDER_UUID = "querybuilderUuid";
    private static final String QUERY_BUILDER_REPORT_ID = "querybuilderReportId";

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Task generateTask() {
        Task task = createTask();
        setTaskClass(task);
        TaskParams taskParams = new TaskParams();
        Source source = generateSource();
        taskParams.setParams(SOURCE_TASK_PARAM, source);
        Destination destination = generateDestination();
        if (destination != null) {
            taskParams.setParams(DESTINATION_TASK_PARAM, destination);
        }
        task.setTaskParams(taskParams);
        setTemplateParams(task);
        return task;
    }

    public abstract Source generateSource();

    public abstract Destination generateDestination();

    private void setTemplateParams(Task task) {
        if (config.containsKey(TEMPLATE_PARAMS)) {
            Map<String, String> kv = parseKeyValue((String)config.get(TEMPLATE_PARAMS));
            task.setTemplateParams(kv);
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

    public void setScriptType(Source source) {
        Map<String, Object> scriptType = (Map<String, Object>) config.getOrDefault(SCRIPT_TYPE, new HashMap());
        if (scriptType.containsKey(SCRIPT_TYPE_SQL_OPTION)) {
            Map<String, Object> sqlOption = (Map<String, Object>)scriptType.get(SCRIPT_TYPE_SQL_OPTION);
            String sql = (String) sqlOption.get(SQL);
            source.setSql(sql);
        } else if (scriptType.containsKey(SCRIPT_TYPE_QUERYBUILDER_REPORT_ID_OPTION)) {
            Map<String, Object> qbReportIdOption = (Map<String, Object>)
                    scriptType.get(SCRIPT_TYPE_QUERYBUILDER_REPORT_ID_OPTION);
            source.setQuerybuilderReportId((String)qbReportIdOption.get(QUERY_BUILDER_REPORT_ID));
        } else if (scriptType.containsKey(SCRIPT_TYPE_QUERYBUILDER_ID_OPTION)) {
            Map<String, Object> qbIdOption = (Map<String, Object>)scriptType.get(SCRIPT_TYPE_QUERYBUILDER_ID_OPTION);
            String uuid = (String)qbIdOption.get(QUERY_BUILDER_UUID);
            source.setQuerybuilderUuid(uuid);
        }
    }


}
