package com.hortonworks.streamline.streams.common.athenax;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.streams.common.athenax.entity.DeployRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.JobDefinition;
import com.hortonworks.streamline.streams.common.athenax.entity.JobStatusRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.StopJobRequest;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
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
        // TODO: stop pretending this is athenax-restbackend
        headers.put(RPC_CALLER, "athenax-restbackend");

        // TODO: extract this from given settings
        headers.put(RPC_SERVICE, "athenax-backend-flink-staging");

        athenaXHeaders = Collections.unmodifiableMap(headers);
    }

    public AthenaXRestAPIClient(String athenaxVmApiRootUrl, Subject subject) {
        this.client = ClientBuilder.newClient(new ClientConfig());
        this.athenaxVmApiRootUrl = athenaxVmApiRootUrl;
        this.subject = subject;
    }

    public boolean validateJob(JobDefinition jobDefinition) throws IOException {
        Object request = JsonClientUtil.convertRequestToJson(jobDefinition);
        String response = doPostRequest(generateTopologyUrl(ATHENAX_VALIDATE_JOB_COMMAND), request, MediaType.TEXT_PLAIN_TYPE);
        LOG.debug("Validate job response: {}", response);
        return "OK".equals(response);
    }

    public String deployJob(DeployRequest deployRequest) throws IOException {
        Object request = JsonClientUtil.convertRequestToJson(deployRequest);
        String response = doPostRequest(generateTopologyUrl(ATHENAX_DEPLOY_JOB_COMMAND), request, MediaType.TEXT_PLAIN_TYPE);
        LOG.debug("Deploy job response: {}", response);
        return response;
    }

    public boolean stopJob(StopJobRequest stopJobRequest) throws IOException {
        Object request = JsonClientUtil.convertRequestToJson(stopJobRequest);
        String response = doPostRequest(generateTopologyUrl(ATHENAX_KILL_JOB_COMMAND), request, MediaType.TEXT_PLAIN_TYPE);
        LOG.debug("Stop job response: {}", response);
        return "CLEANED".equals(response);
    }

    public Map<String, String> jobStatus(JobStatusRequest jobStatusRequest) throws IOException {
        Object request = JsonClientUtil.convertRequestToJson(jobStatusRequest);
        String response = doPostRequest(generateTopologyUrl(ATHENAX_JOB_STATUS_COMMAND), request, MediaType.APPLICATION_JSON_TYPE);
        LOG.debug("Job status response: {}", response);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response, new TypeReference<Map<String, String>>() {});
    }

    private String doPostRequest(final String requestUrl, final Object bodyObject, final MediaType acceptType) {
        LOG.debug("Posting AthenaX request: {}", bodyObject);

        try {
            return Subject.doAs(subject, new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return JsonClientUtil.postEntityWithHeaders(client.target(requestUrl), athenaXHeaders, bodyObject, acceptType, String.class);
                }
            });
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new RuntimeException("Exception while requesting " + requestUrl, e);
            }
            throw e;
        }
    }

    private String generateTopologyUrl(String operation) {
        return athenaxVmApiRootUrl + "/" + operation;
    }
}
