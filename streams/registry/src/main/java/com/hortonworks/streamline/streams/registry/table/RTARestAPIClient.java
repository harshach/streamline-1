package com.hortonworks.streamline.streams.registry.table;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.JsonClientUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RTARestAPIClient implements DataSchemaServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(RTARestAPIClient.class);
    private static final String RPC_SERVICE = "RPC-Service";
    private static final String RPC_CALLER = "RPC-Caller";
    private static final String RTA_TABLE_PATH = "tables/";
    private static final String RTA_TABLE_DEFINITION_PATH = "tables/definitions/";
    private static final String RTA_TABLE_DEPLOY_PATH = "tables/%s/deployments/";

    private final String apiRootUrl;
    private final Subject subject;
    private final Client client;

    private static final Map<String, String> rtaHeaders;
    static {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put(RPC_CALLER, "uworc");
        headersMap.put(RPC_SERVICE, "rtaums-staging");
        rtaHeaders = Collections.unmodifiableMap(headersMap);
    }

    public RTARestAPIClient(String apiRootUrl, Subject subject) {
        this(apiRootUrl, ClientBuilder.newClient(new ClientConfig()), subject);
    }

    public RTARestAPIClient(String apiRootUrl, Client client, Subject subject) {
        this.apiRootUrl = apiRootUrl;
        this.client = client;
        this.subject = subject;
    }

    @Override
    public String getTableSchema(String tableName) {
        String response = doGetRequest(generateRequestUrl(RTA_TABLE_DEFINITION_PATH + tableName));
        LOG.debug("Get RTA table schema response: {}", response);
        return response;
    }

    public Collection<Map<String, String>> getTableDeployStatus(String tableName) throws IOException {
        String response = doGetRequest(String.format(generateRequestUrl(RTA_TABLE_DEPLOY_PATH), tableName));
        LOG.debug("Get RTA table deploy status response: {}", response);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response, new TypeReference<Collection<Map<String, String>>>() {});
    }

    @Override
    public void createTable(Object request) {
        String response = doPostRequest(generateRequestUrl(RTA_TABLE_PATH), request);
        LOG.debug("Create RTA table response: {}", response);
    }

    @Override
    public void deployTable(Object request, String tableName) {
        String response = doPostRequest(String.format(generateRequestUrl(RTA_TABLE_DEPLOY_PATH), tableName), request);
        LOG.debug("Deploy RTA table response: {}", response);
    }

    private String doPostRequest(final String requestUrl, final Object bodyObject) {
        LOG.debug("POST request to RTA: {}", bodyObject);

        try {
            return Subject.doAs(subject, new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return JsonClientUtil.postEntityWithHeaders(client.target(requestUrl), rtaHeaders, bodyObject, MediaType.APPLICATION_JSON_TYPE, String.class);
                }
            });
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new RuntimeException("Exception while requesting " + requestUrl, e);
            }
            throw e;
        }
    }

    private String doGetRequest(final String requestUrl) {
        LOG.debug("GET request to RTA URL: {}", requestUrl);

        try {
            return Subject.doAs(subject, new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return JsonClientUtil.getEntityWithHeaders(client.target(requestUrl), rtaHeaders, MediaType.APPLICATION_JSON_TYPE, String.class);
                }
            });
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new RuntimeException("Exception while requesting " + requestUrl, e);
            }
            throw e;
        }
    }

    private String generateRequestUrl(String path) {
        return apiRootUrl + "/" + path;
    }
}
