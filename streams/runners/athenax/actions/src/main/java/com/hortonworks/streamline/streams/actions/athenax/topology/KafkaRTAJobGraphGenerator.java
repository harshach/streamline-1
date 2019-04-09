package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import com.hortonworks.streamline.streams.layout.component.impl.RTASink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaRTAJobGraphGenerator extends TopologyDagVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaRTAJobGraphGenerator.class);

    private KafkaSource kafkaSource;
    private RTASink rtaSink;

    @Override
    public void visit(StreamlineSource source) {
        LOG.debug("Visiting source: {}", source);
        if (kafkaSource != null) {
            LOG.error("Only single source supported in Kafka-RTA");
        } else if (!(source instanceof KafkaSource)) {
            LOG.error("Only Kafka sources supported in Kafka-RTA");
        } else {
            kafkaSource = (KafkaSource) source;
        }
    }

    @Override
    public void visit(StreamlineSink sink) {
        LOG.debug("Visiting sink: {}", sink);
        if (rtaSink != null) {
            LOG.error("Only single sink supported in Kafka-RTA");
        } else if (!(sink instanceof RTASink)) {
            LOG.error("Only RTA sinks supported in Kafka-RTA");
        } else {
            rtaSink = (RTASink) sink;
        }
    }

    @Override
    public void visit(Edge edge) {
        LOG.debug("Visiting edge: {}", edge);
    }

    public RTASink getRtaSink() {
        return rtaSink;
    }

    public KafkaSource getKafkaSource() {
        return kafkaSource;
    }
}
