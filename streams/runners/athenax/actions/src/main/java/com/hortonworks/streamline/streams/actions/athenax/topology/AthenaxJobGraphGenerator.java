package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.common.athenax.AthenaxConstants;
import com.hortonworks.streamline.streams.common.athenax.entity.Connector;
import com.hortonworks.streamline.streams.common.athenax.entity.DeployRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.JobDefinition;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.StreamlineComponent;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.CassandraSink;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSink;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import com.hortonworks.streamline.streams.layout.component.impl.M3Sink;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.hortonworks.streamline.streams.cluster.register.impl.KafkaServiceRegistrar.PARAM_CLUSTER_NAME;
import static com.hortonworks.streamline.streams.cluster.register.impl.KafkaServiceRegistrar.PARAM_ZOOKEEPER_CONNECT;

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
	private static final String KAFKA_TOPIC = "topic";
	private static final String HEATPIPE_PROTOCOL_PREFIX = "kafka+heatpipe://";


	private EnvironmentService environmentService;
	private TopologyLayout topology;
	private String runAsUser;
	private List<StreamlineSource> streamlineSourceList = new ArrayList<>();
	private List<StreamlineSink> streamlineSinkList = new ArrayList<>();
	private String sql;
	private boolean legalAthenaXJob;

	protected AthenaxJobGraphGenerator(TopologyLayout topology, EnvironmentService environmentService, String asUser) {
		this.environmentService = environmentService;
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
		if (!(source instanceof KafkaSource)) {
			legalAthenaXJob = false;
			LOG.error("Only Kafka sources supported in AthenaX");
		}
		streamlineSourceList.add(source);
	}

	@Override
	public void visit(StreamlineSink sink) {
		LOG.debug("Visiting sink: {}", sink);
		if (!(sink instanceof RTASink || sink instanceof KafkaSink
                || sink instanceof CassandraSink || sink instanceof M3Sink)) {
			legalAthenaXJob = false;
			LOG.error("Only Kafka/M3/RTA/Cassandra sinks supported in AthenaX");
		}
		streamlineSinkList.add(sink);
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

	public List<RTASink> getRTASinkList() {
		List<RTASink> rtaSinkList = new ArrayList<>();
		for (StreamlineSink sink : streamlineSinkList) {
			if (sink instanceof RTASink) {
				rtaSinkList.add((RTASink) sink);
			}
		}
		return rtaSinkList;
	}

	public RTACreateTableRequest extractRTACreateTableRequest(RTASink rtaSink) {
		RTACreateTableRequest request = new RTACreateTableRequest();

		Config rtaSinkConfig = rtaSink.getConfig();

		// TODO: Change to use email in runAsUser when available
		request.setOwner(runAsUser + "@uber.com");
		request.setName(rtaSinkConfig.get(RTAConstants.TABLE_NAME));
		request.setRtaTableMetadata(extractRTATableMetadata(rtaSink));

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

	private RTATableMetadata extractRTATableMetadata(RTASink rtaSink) {
		RTATableMetadata metaData = new RTATableMetadata();

		Config rtaSinkConfig = rtaSink.getConfig();

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

	public RTADeployTableRequest extractRTADeployTableRequest(RTASink rtaSink) {
		RTADeployTableRequest request = new RTADeployTableRequest();
		Namespace namespace = getNamespaceFromComponent(rtaSink);
		Map<String, String> kafkaConfigMap = getKafkaConfigMap(namespace);
		request.setKafkaCluster(kafkaConfigMap.get(PARAM_CLUSTER_NAME));
		return request;
	}

	public JobDefinition extractJobDefinition(String dataCenter, String cluster) throws Exception {
		errorIfIllegalAthenaxJob();

		JobDefinition jobDef = new JobDefinition();
		// input connectors (kafka only)
		jobDef.setInput(getInputConnectors(dataCenter, cluster));

		// output connectors(Kafka/M3/RTA/Cassandra)
		jobDef.setOutput(getOutputConnectors(dataCenter, cluster));

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

	public DeployRequest extractDeployJobRequest(String dataCenter, String cluster) throws Exception {
		DeployRequest request = new DeployRequest();

		request.setJobDefinition(extractJobDefinition(dataCenter, cluster));
		request.setBackfill(false);

		// extract env settings from config
		Config cfg = topology.getConfig();
		request.setDataCenter(dataCenter);
		request.setCluster(cluster);
		request.setYarnContainerCount(cfg.getAny(YARN_CONTAINER_COUNT));
		request.setYarnMemoryPerContainerInMB(cfg.getAny(YARN_CONTAINER_MEM));

		return request;
	}

	private List<Connector> getInputConnectors(String dataCenter, String cluster) {
		// input connectors(kafka only for AthenaX)
		List<Connector> inputConnectors = new ArrayList<>();

		for (StreamlineSource streamlineSource : streamlineSourceList) {
			Namespace namespace = getNamespaceFromComponent(streamlineSource);
			Map<String, String> kafkaConfigMap = getKafkaConfigMap(namespace);
			String zkConnectionStr = kafkaConfigMap.get(PARAM_ZOOKEEPER_CONNECT);

			Config sourceConfig = streamlineSource.getConfig();

			// extract kafka properties
			Properties kafkaProperties = new Properties();
			kafkaProperties.put(BOOTSTRAP_SERVERS, sourceConfig.get("bootstrapServers"));
			kafkaProperties.put(GROUP_ID, getConsumerGroupId(dataCenter, cluster));

			kafkaProperties.put(ENABLE_AUTO_COMMIT, "false");
			kafkaProperties.put(HEATPIPE_APP_ID, topology.getName());
			kafkaProperties.put(HEATPIPE_KAFKA_HOST_PORT, "localhost:18083");
			kafkaProperties.put(HEATPIPE_SCHEMA_SERVICE_HOST_PORT, "localhost:14040");

			String topicName = sourceConfig.get(KAFKA_TOPIC);
			Connector kafkaInput = new Connector();
			kafkaInput.setProperties(kafkaProperties);
			kafkaInput.setType(Connector.KAFKA);
			kafkaInput.setName(topicName);

			kafkaInput.setUri(HEATPIPE_PROTOCOL_PREFIX + getHostPortListOnly(zkConnectionStr) + "/" + topicName);

			inputConnectors.add(kafkaInput);
		}

		return inputConnectors;
	}

	private String getConsumerGroupId(String dataCenter, String cluster) {
		return String.format("%s_%s_%s_%s", UWORC_SERVICE_NAME, topology.getName(), dataCenter, cluster);
	}

	private String getHostPortListOnly(String zkConnectionStr) {
		return zkConnectionStr.split("/")[0];
	}

	private List<Connector> getOutputConnectors(String dataCenter, String cluster) throws Exception {
		List<Connector> outputConnectors = new ArrayList<>();

		for (StreamlineSink sink : streamlineSinkList) {
			if (sink instanceof KafkaSink || sink instanceof RTASink) {
				Config sinkConfig = sink.getConfig();

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
				kafkaOutput.setType(Connector.KAFKA);
				kafkaOutput.setName(topicName);

				kafkaOutput.setUri(HEATPIPE_PROTOCOL_PREFIX + bootStrapServers + "/" + topicName);

				outputConnectors.add(kafkaOutput);
			} else if (sink instanceof CassandraSink) {
				outputConnectors.add(buildCassandraOutput(sink));
			} else if (sink instanceof M3Sink) {
				outputConnectors.add(buildM3Output(sink, dataCenter, cluster));
			}
		}
		return outputConnectors;
	}

	private Connector buildCassandraOutput(StreamlineSink sink) throws Exception {
		Config sinkConfig = sink.getConfig();

		Properties cassandraProperties = new Properties();
		String ttl = sinkConfig.get("ttl");
		if (ttl != null) {
			cassandraProperties.put("ttl", ttl);
		}

		String uriFormat = "cassandra://%s/%s/%s";
		String servers = sinkConfig.get("servers");
		UnsContactPointsResolver unsContactPointsResolver = new UnsContactPointsResolver();
		List<String> hostAddrs
				= unsContactPointsResolver.getContactPoints(servers, "NativeTransport");
		String keyspace = sinkConfig.get("keyspace");
		String table = sinkConfig.get("table");
		String uri = String.format(uriFormat, String.join(",", hostAddrs), keyspace, table);

		Connector cassandraOutput = new Connector();
		cassandraOutput.setName(sinkConfig.get("name"));
		cassandraOutput.setProperties(cassandraProperties);
		cassandraOutput.setType(Connector.CASSANDRA);
		cassandraOutput.setUri(uri);

		return cassandraOutput;
	}

	private Connector buildM3Output(StreamlineSink sink, String dataCenter, String cluster) throws Exception {
		Config sinkConfig = sink.getConfig();

		Properties m3Properties = new Properties();
		ObjectMapper mapper = new ObjectMapper();

		Map<String, String> commonTagMap = new HashMap<>();
		commonTagMap.put(AthenaxConstants.ATHENAX_METRIC_PARAM_DC, dataCenter);
		commonTagMap.put(AthenaxConstants.ATHENAX_METRIC_PARAM_ENV, cluster);
		commonTagMap.put(AthenaxConstants.ATHENAX_METRIC_PARAM_JOB_NAME, topology.getName());
		m3Properties.put("m3.commonTag", mapper.writeValueAsString(commonTagMap));

		List<Map<String, Object>> m3Metrics = sinkConfig.getAny("metrics");
		Map<String, Map<String, String>> fieldTagMap = getFieldTagMap(m3Metrics);
		m3Properties.put("m3.fieldTag", mapper.writeValueAsString(fieldTagMap));
		String uri = getM3Uri(m3Metrics);

		Connector m3Output = new Connector();
		m3Output.setName(sinkConfig.get("outputName"));
		m3Output.setProperties(m3Properties);
		m3Output.setType(Connector.M3);
		m3Output.setUri(uri);

		return m3Output;
	}

	protected static String getM3Uri(List<Map<String, Object>> m3Metrics) {
		String uri = "m3://?";
		Map<String, List<String>> typeNamesMapping = new HashMap<>();
		for (Map<String, Object> m3Metric : m3Metrics) {
			String metricName = (String) m3Metric.get("metricName");
			String metricType = (String) m3Metric.get("metricType");

			typeNamesMapping.putIfAbsent(metricType, new ArrayList<>());
			typeNamesMapping.get(metricType).add(metricName);
		}
		List<String> typeParams = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : typeNamesMapping.entrySet()) {
			typeParams.add(String.format("%s=%s", entry.getKey(), String.join(",", entry.getValue())));
		}
		return uri.concat(String.join("&", typeParams));
	}

	protected static Map<String, Map<String, String>> getFieldTagMap(List<Map<String, Object>> m3Metrics) {
		Map<String, Map<String, String>> fieldTagMap = new HashMap<>();
		for (Map<String, Object> m3Metric : m3Metrics) {
			String metricName = (String) m3Metric.get("metricName");
			String tagString = (String) m3Metric.get("tags");

			Map<String, String> tagValueMap = new HashMap<>();
			for (String splitTagString : tagString.split(",")) {
				String[] tagValue = splitTagString.split(":", 2);
				String tag = tagValue[0].trim();
				String value = tagValue[1].trim();
				tagValueMap.put(tag, value);
			}
			fieldTagMap.putIfAbsent(metricName, tagValueMap);
		}
		return fieldTagMap;
	}

	private void errorIfIllegalAthenaxJob() throws Exception {
		if (!legalAthenaXJob) {
			LOG.error("Failed to convert Topology into AthenaX job.");
			throw new Exception("Failed to convert Topology into AthenaX job.");
		}
	}

	private Namespace getNamespaceFromComponent(StreamlineComponent component) {
		String clusterName = component.getConfig().get("clusters");
		return environmentService.getNamespaceByName(clusterName);
	}

	private Map<String, String> getKafkaConfigMap(Namespace namespace) {
		Map<String, String> configMap;

		Service service = environmentService.getFirstOccurenceServiceForNamespace(namespace, ServiceConfigurations.KAFKA.name());

		if (service == null) {
			throw new IllegalStateException("Kafka is not associated to the namespace " + namespace.getName() + "(" + namespace.getId() + ")");
		}

		ServiceConfiguration serviceConfiguration = environmentService.
				getServiceConfigurationByName(service.getId(), ServiceConfigurations.KAFKA.getConfNames()[0]);
		try {
			configMap = serviceConfiguration.getConfigurationMap();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return configMap;
	}
}
