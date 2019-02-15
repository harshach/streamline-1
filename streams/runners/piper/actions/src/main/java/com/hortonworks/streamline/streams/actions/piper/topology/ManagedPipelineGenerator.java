package com.hortonworks.streamline.streams.actions.piper.topology;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.catalog.CatalogToDeploymentConverter;
import com.hortonworks.streamline.streams.catalog.CatalogToLayoutConverter;
import com.hortonworks.streamline.streams.catalog.TopologyDeployment;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.StreamlineTask;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.piper.PiperTaskComponent;
import com.hortonworks.streamline.streams.layout.piper.PiperTaskComponentFactory;
import com.hortonworks.streamline.streams.piper.common.pipeline.Pipeline;
import com.hortonworks.streamline.streams.piper.common.pipeline.Task;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Topology visitor for generating Piper Managed Pipeline job
 */
public class ManagedPipelineGenerator extends TopologyDagVisitor {

    private static final String PIPER_TOPOLOGY_CONFIG_OWNER = "topology.owner";
    private static final String PIPER_TOPOLOGY_CONFIG_OWNER_LDAP_GROUPS = "topology.ownerLDAPGroups";
    private static final String PIPER_TOPOLOGY_CONFIG_DESCRIPTION = "topology.description";
    private static final String PIPER_TOPOLOGY_CONFIG_EMAIL = "topology.email";
    private static final String PIPER_TOPOLOGY_CONFIG_START_DATE = "topology.startDate";
    private static final String PIPER_TOPOLOGY_CONFIG_END_DATE = "topology.endDate";
    private static final String PIPER_TOPOLOGY_CONFIG_AUTO_BACKFILL = "topology.autobackfill";
    private static final String PIPER_TOPOLOGY_CONFIG_EMAIL_FAILURE = "topology.emailOnFailure";
    private static final String PIPER_TOPOLOGY_CONFIG_EMAIL_RETRY = "topology.emailOnRetry";
    private static final String PIPER_TOPOLOGY_TAGS = "topology.tags";
    private static final String PIPER_TOPOLOGY_EMAILS = "topology.email";

    protected static final String PIPER_TOPOLOGY_CONFIG_DATACENTER_CHOICE_MODE = "topology.datacenterChoiceMode";
    protected static final String PIPER_TOPOLOGY_CONFIG_CHOSEN_DATACENTER = "topology.runChosenDatacenterOption";
    protected static final String PIPER_TOPOLOGY_CONFIG_ALL_DATACENTERS = "topology.runAllDatacentersOption";
    protected static final String PIPER_TOPOLOGY_CONFIG_ONE_DATACENTER = "topology.runOneDatacenterOption";
    protected static final String PIPER_TOPOLOGY_CONFIG_SELECTED_DATACENTER = "topology.selectedDatacenter";

    private static final String PIPER_TOPOLOGY_CONFIG_SECURE_SELECTION = "topology.secureSelection";
    private static final String PIPER_TOPOLOGY_CONFIG_SECURE_OPTION = "topology.secureTrueOption";
    private static final String PIPER_TOPOLOGY_CONFIG_NON_SECURE_OPTION = "topology.secureFalseOption";
    private static final String PIPER_TOPOLOGY_CONFIG_PROXY_USER = "topology.proxyUser";

    // Schedule interval options
    private static final String PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL = "topology.scheduleIntervalSelection";
    private static final String PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TYPE_TIME = "topology.scheduleIntervalTimeOption";
    private static final String PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TYPE_CRON = "topology.scheduleIntervalCronOption";
    private static final String PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TYPE_TRIGGER = "topology.scheduleIntervalTriggerBasedOption";
    private static final String PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_MULTIPLIER = "topology.timeBasedIntervalMultiplier";
    private static final String PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TIME_TYPE = "topology.timeBasedIntervalType";
    private static final String PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_CRON = "topology.scheduleIntervalCron";

    private static final String PIPER_TOPOLOGY_CONFIG_TIME_TYPE_MINUTE = "Minute";
    private static final String PIPER_TOPOLOGY_CONFIG_TIME_TYPE_HOUR = "Hour";
    private static final String PIPER_TOPOLOGY_CONFIG_TIME_TYPE_DAY = "Day";
    private static final String PIPER_TOPOLOGY_CONFIG_TIME_TYPE_WEEK = "Week";


    protected static final String PIPER_CONFIG_DATACENTER_CHOICE_MODE = "datacenter_choice_mode";
    protected static final String PIPER_CONFIG_SELECTED_DATACENTERS = "selected_datacenters";

    protected static final String PIPER_CONFIG_RUN_ONE_DATACENTER = "run_in_one_datacenter";
    protected static final String PIPER_CONFIG_RUN_CHOSEN_DATACENTER = "run_in_chosen_datacenters";
    protected static final String PIPER_CONFIG_RUN_ALL_DATACENTERS = "run_in_all_datacenters";
    private static final String PIPER_CONFIG_TRIGGER_BASED = "external_trigger";
    private static final String PIPER_CONFIG_TIME_INTERVAL = "time_interval";

    private static final String UI_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String PIPER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final long MIN_SCHEDULE_INTERVAL = 120;

    private List<StreamlineSource> sourceList;
    private List<StreamlineSink> sinkList;
    private List<Edge> edgeList;
    private TopologyLayout topology;
    private Map<String, Object> piperDeployment;

    protected ManagedPipelineGenerator(TopologyLayout topology, Map<String, Object> piperDeployment) {
        sourceList = new ArrayList<>();
        sinkList = new ArrayList<>();
        edgeList = new ArrayList<>();
        this.topology = topology;
        this.piperDeployment = piperDeployment;
    }

    @Override
    public void visit(RulesProcessor rulesProcessor) {
    }

    @Override
    public void visit(StreamlineSource source) {
        sourceList.add(source);
    }

    @Override
    public void visit(StreamlineSink sink) {
        sinkList.add(sink);
    }

    @Override
    public void visit(StreamlineProcessor processor) {
    }

    @Override
    public void visit(Edge edge) {
        edgeList.add(edge);
    }

    public Pipeline generatePipeline() {
        Config topologyConfig = this.topology.getConfig();
        Pipeline pipeline = new Pipeline();
        configurePipelineSettings(topologyConfig, pipeline);
        configureTasks(pipeline);
        return pipeline;
    }

    private void configureTasks(Pipeline pipeline) {
        ArrayList<Task> tasks = new ArrayList<Task>();

        PiperTaskComponentFactory taskFactory = new PiperTaskComponentFactory();
        for (Component baseComponent: topology.getTopologyDag().getInputComponents()) {
            StreamlineTask component = (StreamlineTask)baseComponent;

            PiperTaskComponent piperComponent = taskFactory.getPiperTaskComponent(component);
            Map<String, Object> props = new LinkedHashMap<>();
            props.putAll(component.getConfig().getProperties());
            piperComponent.withConfig(props, pipeline);
            Task task = piperComponent.generateTask();
            task.setTaskId(taskIdForComponent(component));

            List<String> dependencies = new ArrayList<String>();
            for (Edge edge: topology.getTopologyDag().getEdgesTo(component)) {
                dependencies.add(taskIdForComponent(edge.getFrom()));
            }
            task.setDependencies(dependencies);
            tasks.add(task);
        }
        pipeline.setTasks(tasks);
    }

    private void configurePipelineSettings(Config topologyConfig, Pipeline pipeline) {
        pipeline.setPipelineName(this.topology.getName());
        pipeline.setOwner(topologyConfig.get(PIPER_TOPOLOGY_CONFIG_OWNER));
        if (topologyConfig.contains(PIPER_TOPOLOGY_CONFIG_DESCRIPTION)) {
            pipeline.setPipelineDescription(topologyConfig.get(PIPER_TOPOLOGY_CONFIG_DESCRIPTION));
        }
        if (topologyConfig.contains(PIPER_TOPOLOGY_CONFIG_OWNER_LDAP_GROUPS)) {
            String groups = topologyConfig.get(PIPER_TOPOLOGY_CONFIG_OWNER_LDAP_GROUPS);
            pipeline.setOwnerLdapGroups(Arrays.asList(groups.split(",")));
        }
        pipeline.setStartDate(convertCalendarToISO(topologyConfig.get(PIPER_TOPOLOGY_CONFIG_START_DATE)));
        if (topologyConfig.contains(PIPER_TOPOLOGY_CONFIG_END_DATE)) {
            pipeline.setEndDate(convertCalendarToISO(topologyConfig.get(PIPER_TOPOLOGY_CONFIG_END_DATE)));
        }
        if (topologyConfig.contains(PIPER_TOPOLOGY_CONFIG_EMAIL)) {
            String email = topologyConfig.get(PIPER_TOPOLOGY_CONFIG_EMAIL);
            if (email.contains(",")) {
                pipeline.setEmail(Arrays.asList(email.split(",")));
            } else {
                pipeline.setEmail(email);
            }
        }
        if (topologyConfig.contains(PIPER_TOPOLOGY_CONFIG_EMAIL_FAILURE)) {
            pipeline.setEmailOnFailure(topologyConfig.getAny(PIPER_TOPOLOGY_CONFIG_EMAIL_FAILURE));
        } else {
            pipeline.setEmailOnFailure(false);
        }
        if (topologyConfig.contains(PIPER_TOPOLOGY_CONFIG_EMAIL_RETRY)) {
            pipeline.setEmailOnRetry(topologyConfig.getAny(PIPER_TOPOLOGY_CONFIG_EMAIL_RETRY));
        } else {
            pipeline.setEmailOnRetry(false);
        }
        if (topologyConfig.contains(PIPER_TOPOLOGY_TAGS)) {
            String tags = topologyConfig.getString(PIPER_TOPOLOGY_TAGS);
            pipeline.setTags(Arrays.asList(tags.split(",")));
        }
        if (topologyConfig.contains(PIPER_TOPOLOGY_EMAILS)) {
            String emails = topologyConfig.getString(PIPER_TOPOLOGY_EMAILS);
            pipeline.setEmail(Arrays.asList(emails.split(",")));
        }
        configureDatacenterOptions(topologyConfig, pipeline);
        configureScheduleInterval(topologyConfig, pipeline);
        configureSecureOptions(topologyConfig, pipeline);
    }

    private void configureSecureOptions(Config topologyConfig, Pipeline pipeline) {
        Map datacenterProp = (Map<String,Object>)topologyConfig.getProperties().get(PIPER_TOPOLOGY_CONFIG_SECURE_SELECTION);
        if (datacenterProp.containsKey(PIPER_TOPOLOGY_CONFIG_SECURE_OPTION)) {
            pipeline.setSecure(true);
            Map secureProps = (Map<String,Object>)datacenterProp.get(PIPER_TOPOLOGY_CONFIG_SECURE_OPTION);
            if (secureProps.containsKey(PIPER_TOPOLOGY_CONFIG_PROXY_USER)) {
                pipeline.setProxyUser((String)secureProps.get(PIPER_TOPOLOGY_CONFIG_PROXY_USER));
            }
        } else if (datacenterProp.containsKey(PIPER_TOPOLOGY_CONFIG_NON_SECURE_OPTION)) {
            pipeline.setSecure(false);
            pipeline.setAutoEnable(true);
        }
    }


    private void configureDatacenterOptions(Config topologyConfig, Pipeline pipeline) {
        String dcChoiceMode = (String) this.piperDeployment.get(PIPER_TOPOLOGY_CONFIG_DATACENTER_CHOICE_MODE);
        Pipeline.DatacenterChoiceMode choiceEnum = Pipeline.DatacenterChoiceMode.fromValue(dcChoiceMode);
        pipeline.setDatacenterChoiceMode(choiceEnum);

        if (this.piperDeployment.containsKey(PIPER_TOPOLOGY_CONFIG_SELECTED_DATACENTER)) {
            List<String> datacenters = (List<String>)
                    this.piperDeployment.get(PIPER_TOPOLOGY_CONFIG_SELECTED_DATACENTER);

            pipeline.setSelectedDatacenters(datacenters);
        }
    }

    private void configureScheduleInterval(Config topologyConfig, Pipeline pipeline) {
        if (topologyConfig.contains(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL)) {
            Map intervalProp = (Map<String,Object>)topologyConfig.getProperties().get(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL);

            String triggerType = PIPER_CONFIG_TIME_INTERVAL;
            if (intervalProp.containsKey(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TYPE_TRIGGER)) {
                triggerType = PIPER_CONFIG_TRIGGER_BASED;
            } else if (intervalProp.containsKey(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TYPE_TIME)) {
                Map<String,Object> intervalOptions = (Map<String,Object>)intervalProp.get(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TYPE_TIME);
                configureAutoBackfill(intervalOptions, pipeline);
                String timeType = (String)intervalOptions.get(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TIME_TYPE);
                long multiplier = Long.parseLong((String)intervalOptions.get(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_MULTIPLIER));
                long intervalTime = 60;

                if (PIPER_TOPOLOGY_CONFIG_TIME_TYPE_MINUTE.equalsIgnoreCase(timeType)) {
                    intervalTime = 60 * multiplier;
                } else if (PIPER_TOPOLOGY_CONFIG_TIME_TYPE_HOUR.equalsIgnoreCase(timeType)) {
                    intervalTime = 60 * 60 * multiplier;
                } else if (PIPER_TOPOLOGY_CONFIG_TIME_TYPE_DAY.equalsIgnoreCase(timeType)) {
                    intervalTime = 60 * 60 * 24 * multiplier;
                } else if (PIPER_TOPOLOGY_CONFIG_TIME_TYPE_WEEK.equalsIgnoreCase(timeType)) {
                    intervalTime = 60 * 60 * 24 * 7 * multiplier;
                }
                if (intervalTime < MIN_SCHEDULE_INTERVAL) {
                    throw new RuntimeException("Schedule interval must be at least " + MIN_SCHEDULE_INTERVAL + " seconds.");
                }
                pipeline.setScheduleInterval(intervalTime);
            } else if (intervalProp.containsKey(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TYPE_CRON)) {
                Map cronOptions = (Map<String,Object>)intervalProp.get(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_TYPE_CRON);
                String cronTab = (String)cronOptions.get(PIPER_TOPOLOGY_CONFIG_SCHEDULE_INTERVAL_CRON);
                pipeline.setScheduleInterval(cronTab);
                configureAutoBackfill(cronOptions, pipeline);
            }
            Pipeline.TriggerType triggerTypeEnum = Pipeline.TriggerType.fromValue(triggerType);
            pipeline.setTriggerType(triggerTypeEnum);
        }
    }

    private void configureAutoBackfill(Map<String, Object> intervalOptions, Pipeline pipeline) {
        if (intervalOptions.containsKey(PIPER_TOPOLOGY_CONFIG_AUTO_BACKFILL)) {
            pipeline.setAutoBackfill((Boolean) intervalOptions.get(PIPER_TOPOLOGY_CONFIG_AUTO_BACKFILL));
        } else {
            pipeline.setAutoBackfill(false);
        }
    }


    private String taskIdForComponent(Component component) {
        return component.getName().replace(' ', '_');
    }

    private String convertCalendarToISO(String dateStr) {
        DateFormat df = new SimpleDateFormat(UI_DATE_FORMAT);
        try {
            Date date = df.parse(dateStr);
            df = new SimpleDateFormat(PIPER_DATE_FORMAT);
            return df.format(date);
        } catch (ParseException e) {
        }
        return null;
    }
}
