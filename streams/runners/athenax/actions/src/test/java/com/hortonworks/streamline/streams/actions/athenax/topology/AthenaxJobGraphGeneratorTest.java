package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.common.athenax.entity.JobDefinition;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.KafkaSource;
import com.hortonworks.streamline.streams.layout.component.impl.RTASink;
import com.hortonworks.streamline.streams.layout.component.impl.SqlProcessor;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(JMockit.class)
public class AthenaxJobGraphGeneratorTest {
  @Mocked
  EnvironmentService environmentService;

  @Test
  public void testDagTraverse() throws Exception {
    ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
    serviceConfiguration.setConfiguration("{\"zookeeper.connect\": \"localhost:2181\"}");
    new Expectations() {{
      environmentService.getServiceConfigurationByName(anyLong, anyString);
      result = serviceConfiguration;
    }};
    // source
    KafkaSource kafkaSource = new KafkaSource();
    kafkaSource.setId("k1");
    kafkaSource.addOutputStream(new Stream("f1", "f2"));
    Config srcConfig = new Config();
    srcConfig.put("bootstrapServers", "localhost:9092");
    srcConfig.put("consumerGroupId", "group1");
    srcConfig.put("topic", "topicSource");
    srcConfig.put("clusters", "test");
    kafkaSource.setConfig(srcConfig);

    // sql processor
    SqlProcessor sqlProcessor = new SqlProcessor();
    String sql = "select f2 from T1";
    sqlProcessor.setSqlStatement(sql);
    sqlProcessor.addOutputStream(new Stream("f2"));

    // sink
    RTASink rtaSink = new RTASink();
    rtaSink.setId("rta");
    Config sinkConfig = new Config();
    sinkConfig.put("bootstrapServers", "localhost:9092");
    sinkConfig.put("topic", "topicSink");
    sinkConfig.put("clusters", "test");
    rtaSink.setConfig(sinkConfig);

    // construct topology DAG: kafkaSource -> sql -> rtaSink
    TopologyDag topologyDag = new TopologyDag();
    topologyDag.add(kafkaSource).add(sqlProcessor).add(rtaSink);
    topologyDag.addEdge(kafkaSource, sqlProcessor);
    topologyDag.addEdge(sqlProcessor, rtaSink);

    Config cfg = new Config();
    String topologyName = "toplogy1";
    TopologyLayout topologyLayout = new TopologyLayout(1L, topologyName, cfg, topologyDag);

    AthenaxJobGraphGenerator requestGenerator = new AthenaxJobGraphGenerator(topologyLayout, environmentService, null);
    topologyDag.traverse(requestGenerator);

    JobDefinition job = requestGenerator.extractJobDefinition();

    // verify
    assertEquals(topologyName, job.jobName());
    assertEquals("uworc", job.serviceName());
    assertEquals(sql, job.transformQuery());
    assertFalse(job.isBackfill());
  }
}
