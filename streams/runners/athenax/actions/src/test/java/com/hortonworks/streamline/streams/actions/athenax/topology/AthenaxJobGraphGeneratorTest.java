package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.actions.athenax.topology.entity.JobDefinition;
import com.hortonworks.streamline.streams.layout.component.*;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSink;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import com.hortonworks.streamline.streams.layout.component.impl.SqlProcessor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AthenaxJobGraphGeneratorTest {
  @Test
  public void testDagTraverse() throws Exception {
    // source
    KafkaSource kafkaSource = new KafkaSource();
    kafkaSource.setId("k1");
    kafkaSource.addOutputStream(new Stream("f1", "f2"));
    Config srcConfig = new Config();
    srcConfig.put("bootstrapServers", "localhost:9092");
    srcConfig.put("consumerGroupId", "group1");
    srcConfig.put("topic", "topicSource");
    kafkaSource.setConfig(srcConfig);

    // sql processor
    SqlProcessor sqlProcessor = new SqlProcessor();
    String sql = "select f2 from T1";
    sqlProcessor.setSqlStatement(sql);
    sqlProcessor.addOutputStream(new Stream("f2"));

    // sink
    KafkaSink kafkaSink = new KafkaSink();
    kafkaSink.setId("k2");
    Config sinkConfig = new Config();
    sinkConfig.put("bootstrapServers", "localhost:9092");
    sinkConfig.put("topic", "topicSink");
    kafkaSink.setConfig(sinkConfig);

    // construct topology DAG: kafkaSource -> sql -> kafkaSink
    TopologyDag topologyDag = new TopologyDag();
    topologyDag.add(kafkaSource).add(sqlProcessor).add(kafkaSink);
    topologyDag.addEdge(kafkaSource, sqlProcessor);
    topologyDag.addEdge(sqlProcessor, kafkaSink);

    Config cfg = new Config();
    String topologyName = "toplogy1";
    TopologyLayout topologyLayout = new TopologyLayout(1L, topologyName, cfg, topologyDag);

    AthenaxJobGraphGenerator requestGenerator = new AthenaxJobGraphGenerator(topologyLayout, null, null);
    topologyDag.traverse(requestGenerator);

    JobDefinition job = requestGenerator.extractJobDefinition("localhost:2181");

    // verify
    assertEquals(topologyName, job.jobName());
    assertEquals("uworc", job.serviceName());
    assertEquals(sql, job.transformQuery());
    assertFalse(job.isBackfill());
  }
}
