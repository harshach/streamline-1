package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.common.athenax.entity.Connector;
import com.hortonworks.streamline.streams.common.athenax.entity.DeployRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.JobDefinition;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSink;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import com.hortonworks.streamline.streams.layout.component.impl.RTASink;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.SqlProcessor;
import com.hortonworks.streamline.streams.registry.table.RTACreateTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTADeployTableRequest;
import com.hortonworks.streamline.streams.registry.table.RTAQueryTypes;
import com.hortonworks.streamline.streams.registry.table.RTATableField;
import com.hortonworks.streamline.streams.registry.table.RTATableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AthenaxJobGraphGenerator extends TopologyDagVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(AthenaxJobGraphGenerator.class);

	private static final String UWORC_SERVICE_NAME = "uworc";
	private static final String YARN_CONTAINER_COUNT = "topology.yarn.containerCount";
	private static final String YARN_CONTAINER_MEM = "topology.yarn.containerMem";
	private static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
	private static final String GROUP_ID = "group.id";
	private static final String CLIENT_ID = "client.id";
	private static final String ENABLE_AUTO_COMMIT = "enable.auto.commit";
	private static final String HEATPIPE_APP_ID = "heatpipe.app_id";
	private static final String HEATPIPE_KAFKA_HOST_PORT = "heatpipe.kafka.hostport";
	private static final String HEATPIPE_SCHEMA_SERVICE_HOST_PORT = "heatpipe.schemaservice.hostport";
	private static final String TYPE_KAFKA = "kafka";
	private static final String KAFKA_TOPIC = "topic";
	private static final String HEATPIPE_PROTOCOL_PREFIX = "kafka+heatpipe://";


	private TopologyLayout topology;
	private String runAsUser;
	private StreamlineSource streamlineSource;
	private StreamlineSink streamlineSink;
	private String sql;
	private boolean legalAthenaXJob;

	protected AthenaxJobGraphGenerator(TopologyLayout topology, String asUser) {
		this.topology = topology;
		this.runAsUser = asUser;
		this.legalAthenaXJob = true;
	}

	@Override
	public void visit(RulesProcessor rulesProcessor) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public void visit(StreamlineSource source) {
		LOG.debug("Visiting source: {}", source);
		if (streamlineSource != null) {
			legalAthenaXJob = false;
			LOG.error("Only single source supported in AthenaX");
		}
		if (!(source instanceof KafkaSource)) {
			legalAthenaXJob = false;
			LOG.error("Only Kafka source supported in AthenaX");
		}
		streamlineSource = source;
	}

	@Override
	public void visit(StreamlineSink sink) {
		LOG.debug("Visiting sink: {}", sink);
		if (streamlineSink != null) {
			legalAthenaXJob = false;
			LOG.error("Only single sink supported in AthenaX");
		}
		if (!(sink instanceof RTASink || sink instanceof KafkaSink)) {
			legalAthenaXJob = false;
			LOG.error("Only Kafka/RTA sinks supported in AthenaX");
		}
		streamlineSink = sink;
	}

	@Override
	public void visit(StreamlineProcessor processor) {
		LOG.debug("Visiting StreamlineProcessor: {}", processor);

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
		LOG.debug("Visiting edge: {}", edge);
	}

	public boolean isRTAEnabled() {
		return streamlineSink instanceof RTASink;
	}

	public RTACreateTableRequest extractRTACreateTableRequest() {
		if (!isRTAEnabled()) {
			LOG.error("Cannot extract RTA create table request when RTA is not enabled");
			return null;
		}

		RTACreateTableRequest request = new RTACreateTableRequest();

		Config rtaSinkConfig = streamlineSink.getConfig();

		// TODO: Change to use email in runAsUser when available
		request.setOwner(runAsUser + "@uber.com");
		request.setName(rtaSinkConfig.get(RTAConstants.TABLE_NAME));
		request.setRtaTableMetadata(extractRTATableMetadata());

		List<RTATableField> rtaTableFields = new ArrayList<>();
		List<Map<String, Object>> tableFieldConfigs = rtaSinkConfig.getAny(RTAConstants.TABLE_FIELDS);
		for (Map<String, Object> fieldConfig : tableFieldConfigs) {
			RTATableField rtaTableField = new RTATableField();

			rtaTableField.setType((String) fieldConfig.get(RTAConstants.TYPE));
			rtaTableField.setName((String) fieldConfig.get(RTAConstants.NAME));
			rtaTableField.setLogicalType((String) fieldConfig.get(RTAConstants.LOGICAL_TYPE));
			rtaTableField.setCardinality((String) fieldConfig.get(RTAConstants.CARDINALITY));
			rtaTableField.setColumnType((String) fieldConfig.get(RTAConstants.COLUMN_TYPE));
			rtaTableField.setDoc((String) fieldConfig.get(RTAConstants.DOC));

			rtaTableFields.add(rtaTableField);
		}
		request.setFields(rtaTableFields);

		return request;
	}

	private RTATableMetadata extractRTATableMetadata() {
		if (!isRTAEnabled()) {
			LOG.error("Cannot extract RTA table metadata when RTA is not enabled");
			return null;
		}

		RTATableMetadata metaData = new RTATableMetadata();

		Config rtaSinkConfig = streamlineSink.getConfig();

		List<String> primaryKeys = new ArrayList<>();
		List<Map<String, Object>> tableFieldConfigs = rtaSinkConfig.getAny(RTAConstants.TABLE_FIELDS);
		for (Map<String, Object> fieldConfig : tableFieldConfigs) {
			if ((boolean) fieldConfig.get(RTAConstants.IS_PRIMARY_KEY)) {
				primaryKeys.add((String) fieldConfig.get(RTAConstants.NAME));
			}
		}
		metaData.setPrimaryKeys(primaryKeys);

		metaData.setIngestionRate(rtaSinkConfig.getAny(RTAConstants.INGESTION_RATE));
		metaData.setRetentionDays(rtaSinkConfig.getAny(RTAConstants.RETENTION_DAYS));

		List<String> queryTypes = new ArrayList<>();
		for (RTAQueryTypes rtaQueryTypes : RTAQueryTypes.values()) {
			if (rtaSinkConfig.getAny(rtaQueryTypes.getUiFieldName())) {
				queryTypes.add(rtaQueryTypes.getRtaQueryTypeName());
			}
		}
		metaData.setQueryTypes(queryTypes);

		// source topic for RTA ingestion, which is the topic defined in RTA sink
		String rtaSourceTopicName = rtaSinkConfig.get(KAFKA_TOPIC);
		metaData.setSourceName(rtaSourceTopicName);

		return metaData;
	}

	public RTADeployTableRequest extractRTADeployTableRequest() {
		if (!isRTAEnabled()) {
			LOG.error("Cannot extract RTA deploy table request when RTA is not enabled");
			return null;
		}

		RTADeployTableRequest request = new RTADeployTableRequest();
		request.setKafkaCluster(streamlineSink.getConfig().get(RTAConstants.KAFKA_CLUSTER));
		return request;
	}

	public JobDefinition extractJobDefinition(String zkConnectionStr) throws Exception {
		errorIfIllegalAthenaxJob();

		JobDefinition jobDef = new JobDefinition();
		// input connectors (kafka only)
		jobDef.setInput(getInputConnectors(zkConnectionStr));

		// output connectors(kafka/RTA only)
		jobDef.setOutput(getOutputConnectors());

		// only stream is supported at this time
		jobDef.setIsBackfill(false);

		// jobName
		jobDef.setJobName(topology.getName());

		// groupName: default null

		// serviceName: ublame service name to send alert
		jobDef.setServiceName(UWORC_SERVICE_NAME);

	  // sql statement
		jobDef.setTransformQuery(sql);

		return jobDef;
	}

	public DeployRequest extractDeployJobRequest(String dataCenter, String cluster, String zkConnectionStr) throws Exception {
		DeployRequest request = new DeployRequest();

		request.setJobDefinition(extractJobDefinition(zkConnectionStr));
		request.setBackfill(false);

		// extract env settings from config
		Config cfg = topology.getConfig();
		request.setDataCenter(dataCenter);
		request.setCluster(cluster);
		request.setYarnContainerCount(cfg.getAny(YARN_CONTAINER_COUNT));
		request.setYarnMemoryPerContainerInMB(cfg.getAny(YARN_CONTAINER_MEM));

		return request;
	}

	private List<Connector> getInputConnectors(String zkConnectionStr) {
		// input connectors(kafka only for AthenaX)
		List<Connector> inputConnectors = new ArrayList<>();

		Config sourceConfig = streamlineSource.getConfig();

		// extract kafka properties
		Properties kafkaProperties = new Properties();
		kafkaProperties.put(BOOTSTRAP_SERVERS, sourceConfig.get("bootstrapServers"));
		kafkaProperties.put(GROUP_ID, sourceConfig.get("consumerGroupId"));

		kafkaProperties.put(ENABLE_AUTO_COMMIT, "false");
		kafkaProperties.put(HEATPIPE_APP_ID, topology.getName());
		kafkaProperties.put(HEATPIPE_KAFKA_HOST_PORT, "localhost:18083");
		kafkaProperties.put(HEATPIPE_SCHEMA_SERVICE_HOST_PORT, "localhost:14040");

		String topicName = sourceConfig.get(KAFKA_TOPIC);
		Connector kafkaInput = new Connector();
		kafkaInput.setProperties(kafkaProperties);
		kafkaInput.setType(TYPE_KAFKA);
		kafkaInput.setName(topicName);

		kafkaInput.setUri(HEATPIPE_PROTOCOL_PREFIX + zkConnectionStr + "/" + topicName);

		inputConnectors.add(kafkaInput);

		return inputConnectors;
	}

	private List<Connector> getOutputConnectors() {
		List<Connector> outputConnectors = new ArrayList<>();

		Config sinkConfig = streamlineSink.getConfig();

		// extract kafka properties
		Properties kafkaProperties = new Properties();
		String bootStrapServers = sinkConfig.get("bootstrapServers");
		kafkaProperties.put(BOOTSTRAP_SERVERS, bootStrapServers);
		kafkaProperties.put(HEATPIPE_APP_ID, topology.getName());
		kafkaProperties.put(HEATPIPE_KAFKA_HOST_PORT, "localhost:18083");
		kafkaProperties.put(HEATPIPE_SCHEMA_SERVICE_HOST_PORT, "localhost:14040");
		kafkaProperties.put(CLIENT_ID, UWORC_SERVICE_NAME + "/" + topology.getName());

		String topicName = sinkConfig.get(KAFKA_TOPIC);
		Connector kafkaOutput = new Connector();
		kafkaOutput.setProperties(kafkaProperties);
		kafkaOutput.setType(TYPE_KAFKA);
		kafkaOutput.setName(topicName);

		kafkaOutput.setUri(HEATPIPE_PROTOCOL_PREFIX + bootStrapServers + "/" + topicName);

		outputConnectors.add(kafkaOutput);

		return outputConnectors;
	}

	private void errorIfIllegalAthenaxJob() throws Exception {
		if (!legalAthenaXJob) {
			LOG.error("Failed to convert Topology into AthenaX job.");
			throw new Exception("Failed to convert Topology into AthenaX job.");
		}
	}
}
