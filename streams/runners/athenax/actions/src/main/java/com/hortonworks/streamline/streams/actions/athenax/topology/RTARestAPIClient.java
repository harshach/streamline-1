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

public class RTARestAPIClient {
    private static final Logger LOG = LoggerFactory.getLogger(RTARestAPIClient.class);
    private static final String RPC_SERVICE = "RPC-Service";
    private static final String RPC_CALLER = "RPC-Caller";
    private static final String RTA_TABLE_PATH = "tables/";
    private static final String RTA_TABLE_DEPLOY_PATH = "tables/deploy/";
    private static final String API_ROOT_URL = "http://127.0.0.1:5436";
    private final Subject subject;
    private final Client client;

    private static final Map<String, String> rtaHeaders;
    static {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put(RPC_CALLER, "uworc");
        headersMap.put(RPC_SERVICE, "rtaums-staging");
        rtaHeaders = Collections.unmodifiableMap(headersMap);
    }

    public RTARestAPIClient(Subject subject) {
        this(ClientBuilder.newClient(new ClientConfig()), subject);
    }

    public RTARestAPIClient(Client client, Subject subject) {
        this.client = client;
        this.subject = subject;
    }

    public String createVirtualTable(Object request) {
        String response = doPostRequest(generateRequestUrl(RTA_TABLE_PATH), request, MediaType.APPLICATION_JSON_TYPE);
        LOG.debug("Create RTA table response: " + response);
        return response;
    }

    public String deployVirtualTable(Object request, String uuid) {
        String response = doPostRequest(generateRequestUrl(RTA_TABLE_DEPLOY_PATH + uuid), request, MediaType.APPLICATION_JSON_TYPE);
        LOG.debug("Deploy RTA table response: " + response);
        return response;
    }

    private String doPostRequest(final String requestUrl, final Object bodyObject, final MediaType acceptType) {
        LOG.debug("Posting request: " + bodyObject);

        try {
            return Subject.doAs(subject, new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return JsonClientUtil.postEntityWithHeaders(client.target(requestUrl), rtaHeaders, bodyObject, acceptType, String.class);
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

    private String generateRequestUrl(String path) {
        return API_ROOT_URL + "/" + path;
    }
}
