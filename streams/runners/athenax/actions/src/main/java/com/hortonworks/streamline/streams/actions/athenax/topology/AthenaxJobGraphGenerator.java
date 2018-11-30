package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.athenax.topology.entity.*;
import com.hortonworks.streamline.streams.layout.component.*;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSink;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.SqlProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AthenaxJobGraphGenerator extends TopologyDagVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(AthenaxJobGraphGenerator.class);

	private static String ATHENAX_SERVICE_NAME = "athenax";
	private static String YARN_CONTAINER_COUNT = "yarnContainerCount";
	private static String SLOT_PER_YARN_CONTAINER = "slotPerYarnContainer";
	private static String YARN_MEMORY_PER_CONTAINER_IN_MB = "yarnMemoryPerContainerInMB";
	private static String BOOTSTRAP_SERVERS = "bootstrap.servers";
	private static String GROUP_ID = "group.id";
	private static String ENABLE_AUTO_COMMIT = "enable.auto.commit";
	private static String HEATPIPE_APP_ID = "heatpipe.app_id";
	private static String HEATPIPE_KAFKA_HOST_PORT = "heatpipe.kafka.hostport";
	private static String HEATPIPE_SCHEMA_SERVICE_HOST_PORT = "heatpipe.schemaservice.hostport";
	private static String TYPE_KAFKA = "kafka";
	private static String KAFKA_TOPIC = "topic";

	private TopologyLayout topology;
	private TopologyActionContext topologyActionContext;
	private String runAsUser;
	private List<StreamlineSource> sourceList;
	private List<StreamlineSink> sinkList;
	private String sql;
	private boolean legalAthenaXJob;

	protected AthenaxJobGraphGenerator(TopologyLayout topology, TopologyActionContext ctx, String asUser) {
		this.topology = topology;
		this.topologyActionContext = ctx;
		this.runAsUser = asUser;
		this.sourceList = new ArrayList<>();
		this.sinkList = new ArrayList<>();
		this.legalAthenaXJob = true;
	}

	@Override
	public void visit(RulesProcessor rulesProcessor) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public void visit(StreamlineSource source) {
		LOG.debug("visit source: " + source);
		if (source instanceof KafkaSource) {
			sourceList.add(source);
		} else {
			legalAthenaXJob = false;
			LOG.error("non kafka source not supported in AthenaX");
		}
	}

	@Override
	public void visit(StreamlineSink sink) {
		LOG.debug("visit sink: " + sink);

		// TODO: relax this
		if (sink instanceof KafkaSink) {
			sinkList.add(sink);
		} else {
			legalAthenaXJob = false;
			LOG.error("non kafka sink not supported at this time.");
		}
	}

	@Override
	public void visit(StreamlineProcessor processor) {
		LOG.debug("visit StreamlineProcessor:" + processor);

		if (processor instanceof SqlProcessor) {
			SqlProcessor sqlProcessor = (SqlProcessor) processor;
			sql = sqlProcessor.getSqlStatement();
		} else {
			legalAthenaXJob = false;
			LOG.error("Non SqlProcessor not supported in AthenaX");
		}
	}

	@Override
	public void visit(Edge edge) {
		LOG.debug("visit edge:" + edge);
	}

	public JobDefinition extractJobDefinition() throws Exception {
		errorIfIllegalAthenaxJob();

		JobDefinition jobDef = new JobDefinition();
		// input connectors (kafka only)
		jobDef.setInput(getInputConnectors());

		// output connectors(kafka only)
		jobDef.setOutput(getOutputConnectors());

		// only stream is supported at this time
		jobDef.setIsBackfill(false);

		// jobName
		jobDef.setJobName(topology.getName());

		// groupName: default null

		// serviceName: ublame service name to send alert
		jobDef.setServiceName(ATHENAX_SERVICE_NAME);

	  // sql statement
		jobDef.setTransformQuery(sql);

		return jobDef;
	}

	public DeployRequest extractDeployJobRequest(String dataCenter, String cluster) throws Exception {
		DeployRequest request = new DeployRequest();

		request.setJobDefinition(extractJobDefinition());
		request.setBackfill(false);

		// extract env settings from config
		Config cfg = topology.getConfig();
		request.setDataCenter(dataCenter);
		request.setCluster(cluster);
		request.setYarnContainerCount(cfg.getInt(YARN_CONTAINER_COUNT, 1));
		request.setSlotPerYarnContainer(cfg.getInt(SLOT_PER_YARN_CONTAINER, 1));
		request.setYarnMemoryPerContainerInMB(cfg.getInt(YARN_MEMORY_PER_CONTAINER_IN_MB, 1024));

		return request;
	}

	public StopJobRequest extractStopJobRequest(String applicationId, String dataCenter, String cluster) {
		StopJobRequest request = new StopJobRequest();
		request.setDataCenter(dataCenter);
		request.setCluster(cluster);
		request.setAppId(applicationId);
		return request;
	}

	public JobStatusRequest extractJobStatusRequest(String applicationId, String dataCenter, String cluster){
		JobStatusRequest request = new JobStatusRequest();
		request.setDataCenter(dataCenter);
		request.setCluster(cluster);
		request.setYarnApplicationId(applicationId);
		return request;
	}

	private List<Connector> getInputConnectors() throws Exception {
		// input connectors(kafka only for AthenaX)
		List<Connector> inputConnectors = new ArrayList<>();
		for (StreamlineSource streamSource : sourceList) {
			KafkaSource kafkaSource;
			if (streamSource instanceof KafkaSource) {
				kafkaSource = (KafkaSource) streamSource;
			} else {
				LOG.error("Topology with non-kakfa inputs can't be converted into AthenaX job.");
				throw new Exception("Topology with non-kakfa inputs can't be converted into AthenaX job.");
			}

			// extract kafka properties
			Properties kafkaProperties = new Properties();
			kafkaProperties.put(BOOTSTRAP_SERVERS, kafkaSource.getConfig().get("bootstrapServers"));
			kafkaProperties.put(GROUP_ID, kafkaSource.getConfig().get("consumerGroupId"));

			kafkaProperties.put(ENABLE_AUTO_COMMIT, "false");
			kafkaProperties.put(HEATPIPE_APP_ID, topology.getName());
			kafkaProperties.put(HEATPIPE_KAFKA_HOST_PORT, "localhost:18083");
			kafkaProperties.put(HEATPIPE_SCHEMA_SERVICE_HOST_PORT, "localhost:14040");

			String topicName = kafkaSource.getConfig().get(KAFKA_TOPIC);
			Connector kafkaInput = new Connector();
			kafkaInput.setProperties(kafkaProperties);
			kafkaInput.setType(TYPE_KAFKA);
			kafkaInput.setName(topicName);

			// TODO: need new config for zookeeper
			//kafkaInput.setUri("kafka+heatpipe://kloakzk09-sjc1:2181,kloakzk10-sjc1:2181,kloakzk05-sjc1:2181,kloakzk03-sjc1:2181,kloakzk04-sjc1:2181/" + topicName);

			inputConnectors.add(kafkaInput);
		}

		return inputConnectors;
	}

	private List<Connector> getOutputConnectors() throws Exception {
		List<Connector> outputConnectors = new ArrayList<>();
		for (StreamlineSink streamSink : sinkList) {
			KafkaSink kafkaSink;
			if (streamSink instanceof KafkaSink) {
				kafkaSink = (KafkaSink) streamSink;
			} else {
				LOG.error("Topology with non-kakfa output can't be converted into AthenaX job at this time.");
				throw new Exception("Topology with non-kakfa output can't be converted into AthenaX job at this time.");
			}

			// extract kafka properties
			Properties kafkaProperties = new Properties();
			String bootStrapServers = kafkaSink.getConfig().get("bootstrapServers");
			kafkaProperties.put(BOOTSTRAP_SERVERS, bootStrapServers);

			String topicName = kafkaSink.getConfig().get(KAFKA_TOPIC);
			Connector kafkaOutput = new Connector();
			kafkaOutput.setProperties(kafkaProperties);
			kafkaOutput.setType(TYPE_KAFKA);
			kafkaOutput.setName(topicName);

			kafkaOutput.setUri("kafka+json://" +  bootStrapServers + "/" + topicName);

			outputConnectors.add(kafkaOutput);
		}

		return outputConnectors;
	}

	private void errorIfIllegalAthenaxJob() throws Exception {
		if (!legalAthenaXJob) {
			LOG.error("Failed to convert Topology into AthenaX job.");
			throw new Exception("Failed to convert Topology into AthenaX job.");
		}
	}
}
