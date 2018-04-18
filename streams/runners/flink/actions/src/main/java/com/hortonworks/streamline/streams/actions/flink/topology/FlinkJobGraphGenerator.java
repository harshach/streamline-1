package com.hortonworks.streamline.streams.actions.flink.topology;

import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSink;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer08;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer08;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FlinkJobGraphGenerator extends TopologyDagVisitor {
	private List<StreamlineSource> sourceList;
	private List<StreamlineSink> sinkList;
	private List<Edge> edgeList;

	protected FlinkJobGraphGenerator() {
		sourceList = new ArrayList<>();
		sinkList = new ArrayList<>();
		edgeList = new ArrayList<>();
	}

	@Override
	public void visit(RulesProcessor rulesProcessor) {
		throw new UnsupportedOperationException("Not Implemented");
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
		throw new UnsupportedOperationException("Not Implemented");
	}

	@Override
	public void visit(Edge edge) {
		edgeList.add(edge);
	}

	public void generateStreamGraph(StreamExecutionEnvironment execEnv) {
		KafkaSource kafkaSource = (KafkaSource)sourceList.get(0);
		Properties srcProperties = new Properties();
		srcProperties.put("bootstrap.servers", kafkaSource.getConfig().get("bootstrapServers"));
		srcProperties.put("group.id", kafkaSource.getConfig().get("consumerGroupId"));
		srcProperties.put("zookeeper.connect", "localhost:2181");
		FlinkKafkaConsumer08 consumer010 = new FlinkKafkaConsumer08(
				kafkaSource.getConfig().get("topic"),
				new SimpleStringSchema(),
				srcProperties);
		DataStream stream = execEnv.addSource(consumer010);

		KafkaSink kafkaSink = (KafkaSink)sinkList.get(0);
		Properties sinkProperties = new Properties();
		sinkProperties.put("bootstrap.servers", kafkaSink.getConfig().get("bootstrapServers"));
		sinkProperties.put("zookeeper.connect", "localhost:2181");
		stream.addSink(new FlinkKafkaProducer08(
				kafkaSink.getConfig().get("topic"),
				new SimpleStringSchema(),
				sinkProperties));
	}
}
