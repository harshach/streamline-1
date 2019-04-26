package com.hortonworks.streamline.streams.layout.piper.contract;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.piper.common.pipeline.contract.Destination;
import com.hortonworks.streamline.streams.piper.common.pipeline.contract.Source;

import java.util.HashMap;
import java.util.Map;

public class HivescriptTaskComponent extends AbstractPiperContractTaskComponent {

    private static final String TASK_PARAMS_HIVE_CLI_CONN_ID = "task_params.hive_cli_conn_id";

    @Override
    public Source generateSource() {
        Source source = new Source();
        source.setType(Source.SourceType.HIVE);
        setScriptType(source);
        source.setConnectionId((String)config.get(TASK_PARAMS_HIVE_CLI_CONN_ID));
        return source;
    }

    @Override
    public Destination generateDestination() {
        return null;
    }

}
