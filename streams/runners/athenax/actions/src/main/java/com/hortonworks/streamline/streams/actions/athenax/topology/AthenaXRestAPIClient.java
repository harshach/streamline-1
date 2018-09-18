package com.hortonworks.streamline.streams.actions.athenax.topology;

import com.hortonworks.streamline.common.JsonClientUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AthenaXRestAPIClient {
  private static final Logger LOG = LoggerFactory.getLogger(AthenaXRestAPIClient.class);

  private static final String RPC_SERVICE = "RPC-Service";
  private static final String RPC_CALLER = "RPC-Caller";
  private static final String ATHENAX_VALIDATE_JOB_COMMAND = "validate/job";
  private static final String ATHENAX_DEPLOY_JOB_COMMAND = "deploy";
  private static final String ATHENAX_KILL_JOB_COMMAND = "stop";
  private static final String ATHENAX_JOB_STATUS_COMMAND = "status";

  private final String athenaxVmApiRootUrl;
  private final Subject subject;
  private final Client client;

  // headers for athenax-vm
  private static final Map<String, String> athenaXHeaders;
  static {
    Map<String, String> headers = new HashMap<>();
    headers.put(RPC_CALLER, "athenax-restbackend");

    // TODO: extract this from given settings
    headers.put(RPC_SERVICE, "athenax-backend-flink-staging");

    athenaXHeaders = Collections.unmodifiableMap(headers);
  }

  public AthenaXRestAPIClient(String athenaxVmApiRootUrl, Subject subject) {
    this(ClientBuilder.newClient(new ClientConfig()), athenaxVmApiRootUrl, subject);
  }

  public AthenaXRestAPIClient(Client client, String athenaxVmApiRootUrl, Subject subject) {
    this.client = client;
    this.athenaxVmApiRootUrl = athenaxVmApiRootUrl;
    this.subject = subject;
  }

  public boolean validateJob(Object request) {
    String response = doPostRequest(generateTopologyUrl(athenaxVmApiRootUrl, ATHENAX_VALIDATE_JOB_COMMAND), athenaXHeaders, request, MediaType.TEXT_PLAIN_TYPE);
    LOG.debug("Deploy job response: " + response);
    return "OK".equals(response);
  }

  public String deployJob(Object request) {
    String response = doPostRequest(generateTopologyUrl(athenaxVmApiRootUrl, ATHENAX_DEPLOY_JOB_COMMAND), athenaXHeaders, request, MediaType.TEXT_PLAIN_TYPE);
    LOG.debug("Deploy job response: " + response);
    return response;
  }

  public boolean stopJob(Object request) {
    String response = doPostRequest(generateTopologyUrl(athenaxVmApiRootUrl, ATHENAX_KILL_JOB_COMMAND), athenaXHeaders, request, MediaType.TEXT_PLAIN_TYPE);
    LOG.debug("Stop job response: " + response);
    return "CLEANED".equals(response);
  }

  public String jobStatus(Object request) {
    String response = doPostRequest(generateTopologyUrl(athenaxVmApiRootUrl, ATHENAX_JOB_STATUS_COMMAND), athenaXHeaders, request, MediaType.APPLICATION_JSON_TYPE);
    LOG.debug("Job status response: " + response);
    return response;
  }

  private String doPostRequest(final String requestUrl, final Map<String, String> headers, final Object bodyObject, final MediaType acceptType) {
    LOG.debug("Posting AthenaX request: " + (String)bodyObject);

    try {
      return Subject.doAs(subject, new PrivilegedAction<String>() {
        @Override
        public String run() {
          return JsonClientUtil.postEntityWithHeaders(client.target(requestUrl), headers, bodyObject, acceptType, String.class);
        }
      });
    } catch (javax.ws.rs.ProcessingException e) {
      if (e.getCause() instanceof IOException) {
        throw new RuntimeException("Exception while requesting " + requestUrl, e);
      }
      throw e;
    } catch (WebApplicationException e) {
      throw e;
    }
  }

  private String generateTopologyUrl(String topologyId, String operation) {
    String baseUrl = athenaxVmApiRootUrl + "/" + operation;
    return baseUrl;
  }
}
