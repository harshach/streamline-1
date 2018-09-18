package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.streams.actions.athenax.topology.entity.*;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AthenaXRestAPIClientTest {
  public static void main(String[] args) {
    String athenaXAPIRootUrl = "http://127.0.0.1:5436";
    Subject subject = null;
    AthenaXRestAPIClient client = new AthenaXRestAPIClient(athenaXAPIRootUrl, subject);

    if (args.length == 0) {
      System.out.println("Error: Need operation name.");
      System.exit(0);
    }

    try {
      if (args[0].equals("deploy")) {
        Object request = AthenaXRestAPIClientTest.getTestDeployRequest();
        client.deployJob(request);

      } else if (args[0].equals("validate")) {

        Object request = AthenaXRestAPIClientTest.getTestValidateRequest();
        client.validateJob(request);
      } else if (args[0].equals("kill")) {
        if (args.length == 1) {
          System.out.println("Need app id");
          System.exit(0);
        }
        Object request = AthenaXRestAPIClientTest.getTestStopRequest(args[1]);
        client.stopJob(request);
      } else if (args[0].equals("status")) {
        if (args.length == 1) {
          System.out.println("Need app id");
          System.exit(0);
        }
        Object request = AthenaXRestAPIClientTest.getTestJobStatusRequest(args[1]);
        client.jobStatus(request);
      } else {
        System.out.print("Unsupported operations");
      }
    } catch (Exception e) {
      System.out.print("Exception:");
      e.printStackTrace();
    }
  }

  private static JobDefinition getTestJobDef() {
    // input
    Connector kafkaInput = new Connector();
    kafkaInput.setType("kafka");
    kafkaInput.setName("hp-uql-tagging-tag_metadata");
    kafkaInput.setUri("kafka+heatpipe://kloakzk09-sjc1:2181,kloakzk10-sjc1:2181,kloakzk05-sjc1:2181,kloakzk03-sjc1:2181,kloakzk04-sjc1:2181/hp-uql-tagging-tag_metadata");
    Properties prop1 = new Properties();
    prop1.setProperty("bootstrap.servers","kloak430-sjc1:9092,kloak509-sjc1:9092,kloak443-sjc1:9092,kloak208-sjc1:9092,kloak207-sjc1:9092,kloak114-sjc1:9092,kloak66-sjc1:9092,kloak112-sjc1:9092,kloak118-sjc1:9092,kloak116-sjc1:9092,kloak105-sjc1:9092,kloak211-sjc1:9092,kloak106-sjc1:9092,kloak72-sjc1:9092,kloak110-sjc1:9092,kloak436-sjc1:9092,kloak205-sjc1:9092,kloak699-sjc1:9092,kloak117-sjc1:9092,kloak670-sjc1:9092,kloak672-sjc1:9092,kloak431-sjc1:9092,kloak434-sjc1:9092,kloak65-sjc1:9092,kloak70-sjc1:9092,kloak22-sjc1:9092,kloak696-sjc1:9092,kloak115-sjc1:9092,kloak24-sjc1:9092,kloak442-sjc1:9092,kloak432-sjc1:9092,kloak21-sjc1:9092,kloak698-sjc1:9092,kloak69-sjc1:9092,kloak210-sjc1:9092,kloak71-sjc1:9092,kloak18-sjc1:9092,kloak107-sjc1:9092,kloak109-sjc1:9092,kloak437-sjc1:9092,kloak675-sjc1:9092,kloak673-sjc1:9092,kloak20-sjc1:9092,kloak697-sjc1:9092,kloak674-sjc1:9092,kloak438-sjc1:9092,kloak441-sjc1:9092,kloak111-sjc1:9092,kloak206-sjc1:9092,kloak671-sjc1:9092,kloak19-sjc1:9092,kloak119-sjc1:9092,kloak444-sjc1:9092,kloak676-sjc1:9092,kloak440-sjc1:9092,kloak23-sjc1:9092,kloak677-sjc1:9092,kloak108-sjc1:9092,kloak433-sjc1:9092,kloak68-sjc1:9092,kloak447-sjc1:9092,kloak209-sjc1:9092,kloak204-sjc1:9092,kloak435-sjc1:9092,kloak113-sjc1:9092,kloak67-sjc1:9092,kloak17-sjc1:9092,kloak678-sjc1:9092,kloak120-sjc1:9092");
    prop1.setProperty("enable.auto.commit", "false");
    prop1.setProperty("heatpipe.app_id", "cdavid_test");
    prop1.setProperty("heatpipe.kafka.hostport", "localhost:18083");
    prop1.setProperty("heatpipe.schemaservice.hostport", "localhost:14040");
    kafkaInput.setProperties(prop1);

    List<Connector> inputConnectors = new ArrayList<>();
    inputConnectors.add(kafkaInput);

    Connector kafkaOutput = new Connector();
    kafkaOutput.setType("kafka");
    kafkaOutput.setName("cdavid-test-tag-metadata");
    kafkaOutput.setUri("kafka+json://kloak683-sjc1:9092,kloak648-sjc1:9092,kloak178-sjc1:9092,kloak180-sjc1:9092,kloak257-sjc1:9092,kloak181-sjc1:9092,kloak41-sjc1:9092,kloak43-sjc1:9092,kloak179-sjc1:9092,kloak259-sjc1:9092,kloak42-sjc1:9092/cdavid-test-tag-metadata");
    Properties prop2 = new Properties();
    prop2.setProperty("bootstrap.servers","kloak683-sjc1:9092,kloak648-sjc1:9092,kloak178-sjc1:9092,kloak180-sjc1:9092,kloak257-sjc1:9092,kloak181-sjc1:9092,kloak41-sjc1:9092,kloak43-sjc1:9092,kloak179-sjc1:9092,kloak259-sjc1:9092,kloak42-sjc1:9092");
    prop2.setProperty("group.id", "athenax__cdavid_test_sjc1_staging");
    kafkaOutput.setProperties(prop2);

    List<Connector> outputConnectors = new ArrayList<>();
    outputConnectors.add(kafkaOutput);

    JobDefinition jobDef = new JobDefinition();
    jobDef.setInput(inputConnectors);
    jobDef.setOutput(outputConnectors);
    jobDef.setJobName("cdavid_test");
    jobDef.setGroupName(null);
    jobDef.setServiceName("aakafkaoffsetmgmt");
    jobDef.setIsBackfill(false);
    jobDef.setTransformQuery("select * from hdrone.hp_uql_tagging_tag_metadata");

    return jobDef;
  }

  private static Object getTestValidateRequest() throws Exception {
    JobDefinition job = getTestJobDef();
    return JsonClientUtil.convertRequestToJson(job);
  }

  private static Object getTestDeployRequest() throws Exception {
    DeployRequest request = new DeployRequest();
    request.setJobDefinition(getTestJobDef());
    request.setDataCenter("sjc1");
    request.setCluster("staging");
    request.setBackfill(false);
    request.setSlotPerYarnContainer(1);
    request.setYarnContainerCount(2);
    request.setYarnMemoryPerContainerInMB(2048);
    request.setYarnQueue("athenax-default");

    // convert to json format
    return JsonClientUtil.convertRequestToJson(request);
  }

  private static Object getTestJobStatusRequest(String appId) throws Exception {
    JobStatusRequest request = new JobStatusRequest();

    request.setDataCenter("sjc1");
    request.setCluster("staging");
    request.setYarnApplicationId(appId);

    // convert to json format
    return JsonClientUtil.convertRequestToJson(request);
  }

  private static Object getTestStopRequest(String appId) throws Exception {
    StopJobRequest request = new StopJobRequest();

    request.setDataCenter("sjc1");
    request.setCluster("staging");
    request.setAppId(appId);

    return JsonClientUtil.convertRequestToJson(request);
  }
}
