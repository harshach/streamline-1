package com.hortonworks.streamline.streams.piper.common;

import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.WrappedWebApplicationException;
import com.uber.engsec.upkiclient.UPKIClient;
import com.uber.engsec.upkiclient.UPKIClientFactory;
import com.uber.engsec.upkiclient.UPKITokenService;
import com.uber.engsec.upkiclient.config.SpiffeConfig;
import com.uber.engsec.upkiclient.utoken.UToken;
import com.uber.engsec.upkiclient.utoken.UTokenException;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import static com.hortonworks.streamline.streams.piper.common.PiperConstants.X_UBER_SOURCE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.UWORC_UBER_SERVICE_NAME;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.HEADER_UPKI_TOKEN;


public class PiperRestAPIClient {
    private static final Logger LOG = LoggerFactory.getLogger(PiperRestAPIClient.class);

    public static final MediaType REST_API_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE;
    public static final Integer DEFAULT_PAGE_SIZE = 2000;

    private final String apiRootUrl;
    private final Subject subject;
    private final Client client;
    private UPKITokenService upkiTokenService;
    private final String upkiSpiffeEndPoint = "/var/cache/udocker_mnt/worf.sock";



    public PiperRestAPIClient(String apiRootUrl, Subject subject) {
        this(ClientBuilder.newClient(new ClientConfig()), apiRootUrl, subject);
    }

    public PiperRestAPIClient(Client client, String apiRootUrl, Subject subject) {
        this.client = client;
        this.apiRootUrl = apiRootUrl;
        this.subject = subject;
        try {
            SpiffeConfig.SpiffeConfigBuilder builder = new SpiffeConfig.SpiffeConfigBuilder().
                    withSpiffeEndpointSocket(upkiSpiffeEndPoint);
            UPKIClient upkiClient = UPKIClientFactory.getUPKIClient(builder);
            this.upkiTokenService = new UPKITokenService(upkiClient);
        } catch (Exception e ) {
            LOG.error("Failed to configure upkiTokenService ",e);
        }
    }

    public Map getManagedPipeline(String uuid) {
        return doGetRequest(String.format("%s/api/v1/managed_pipelines/%s", this.apiRootUrl, uuid));
    }

    public String deployPipeline(Object pipeline) {
        return doPostRequest(String.format("%s/api/v1/managed_pipelines", this.apiRootUrl), pipeline);
    }

    public String redeployPipeline(Object pipeline, String uuid) {
        return doPutRequest(String.format("%s/api/v1/managed_pipelines/%s", this.apiRootUrl, uuid), pipeline);
    }

    public Map getConnections(String type) {
        return doGetRequest(String.format("%s/api/v1/connections/search?page_size=%d&conn_type=%s",
                this.apiRootUrl, DEFAULT_PAGE_SIZE, encodeParam(type)));
    }

    public Map getPools() {
        return doGetRequest(String.format("%s/api/v1/pools/search?page_size=%d",
                this.apiRootUrl, DEFAULT_PAGE_SIZE));
    }

    public Map getPipelineRuns(String uuid, Long from, Long to, Integer page, Integer pageSize) {
        return doGetRequest(String.format(
                "%s/api/v1/pipelines/%s/runs/search?page=%d&page_size=%d&start_date=%d&end_date=%d&order_by=desc(execution_date)",
                this.apiRootUrl, uuid, page, pageSize, from, to));
    }

    public Map getPipelineState(String uuid) {
        Map response =  doGetRequest(String.format("%s/api/v1/pipeline/state?pipeline_id=%s",
                this.apiRootUrl, uuid));
        return PiperUtil.fixExecutionDate(response);
    }

    public Map getDetailedPipelineState(String uuid) {
        Map response =  doGetRequest(String.format("%s/api/v1/pipeline/state?pipeline_id=%s&extras=True",
                this.apiRootUrl, uuid));
        return PiperUtil.fixExecutionDate(response);
    }

    public Map getTaskGraph(String uuid, String executionDate) {
        return doGetRequest(String.format("%s/api/v1/pipeline/task_graph?pipeline_id=%s&execution_date=%s",
                this.apiRootUrl, uuid, executionDate));
    }

    public String deactivatePipeline(String uuid) {
        return doPutRequest(String.format("%s/api/v1/managed_pipelines/%s/deactivate", this.apiRootUrl, uuid), "");
    }

    private String encodeParam(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    private String doPutRequest(final String requestUrl, final Object bodyObject) {
        try {

            Map<String, String> headers = new HashMap<>();
            headers.put(X_UBER_SOURCE, UWORC_UBER_SERVICE_NAME);
            UToken uToken = generateUToken();
            if (uToken != null) {
                headers.put(HEADER_UPKI_TOKEN, uToken.toString());
            }

            return Subject.doAs(subject, new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return JsonClientUtil.putEntity(client.target(requestUrl), headers, bodyObject,
                            REST_API_MEDIA_TYPE, String.class);
                }
            });
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new RuntimeException("Exception while requesting " + requestUrl, e);
            }

            throw e;
        } catch (WebApplicationException e) {
            LOG.error(e.getResponse().readEntity(String.class));
            throw new RuntimeException("Deployment Exception " + e.getResponse().readEntity(String.class), e);
        }
    }

    private String doPostRequest(final String requestUrl, final Object bodyObject) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(X_UBER_SOURCE, UWORC_UBER_SERVICE_NAME);
            UToken uToken = generateUToken();
            if (uToken != null) {
                headers.put(HEADER_UPKI_TOKEN, uToken.toString());
            }


            return Subject.doAs(subject, new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return JsonClientUtil.postEntityWithHeaders(client.target(requestUrl), headers,
                            bodyObject, REST_API_MEDIA_TYPE, String.class);
                }
            });
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new RuntimeException("Exception while requesting " + requestUrl, e);
            }

            throw e;
        } catch (WebApplicationException e) {
            LOG.error(e.getResponse().readEntity(String.class));
            throw new RuntimeException("Deployment Exception " + e.getResponse().readEntity(String.class), e);
        }
    }

    private Map doGetRequest(final String requestUrl) {
        try {
            LOG.debug("GET request to Piper: " + requestUrl);

            Map<String, String> headers = new HashMap<>();
            headers.put(X_UBER_SOURCE, UWORC_UBER_SERVICE_NAME);
            UToken uToken = generateUToken();
            if (uToken != null) {
                headers.put(HEADER_UPKI_TOKEN, uToken.toString());
            }

            return Subject.doAs(subject, new PrivilegedAction<Map>() {
                @Override
                public Map run() {
                    return JsonClientUtil.getEntity(client.target(requestUrl), headers, REST_API_MEDIA_TYPE, Map.class);
                }
            });
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            // JsonClientUtil wraps exception, so need to compare
            if (cause instanceof javax.ws.rs.ProcessingException) {
                if (ex.getCause().getCause() instanceof IOException) {
                    throw new RuntimeException("Exception while requesting " + requestUrl, ex);
                }
            } else if (cause instanceof WebApplicationException) {
                throw WrappedWebApplicationException.of((WebApplicationException)cause);
            }

            throw ex;
        }
    }

    private UToken generateUToken() {
        UToken uToken = null;
        try {
            uToken = upkiTokenService.createSingleHop("piper");
        } catch (UTokenException e) {
            LOG.error("Failed to create a utoken", e);
        }
        return uToken;
    }
}
