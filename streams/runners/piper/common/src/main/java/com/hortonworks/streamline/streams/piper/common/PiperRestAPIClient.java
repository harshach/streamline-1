package com.hortonworks.streamline.streams.piper.common;

import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.WrappedWebApplicationException;
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
import java.util.Map;

public class PiperRestAPIClient {
    private static final Logger LOG = LoggerFactory.getLogger(PiperRestAPIClient.class);

    public static final MediaType REST_API_MEDIA_TYPE = MediaType.TEXT_PLAIN_TYPE;
    public static final Integer DEFAULT_PAGE_SIZE = 2000;

    private final String apiRootUrl;
    private final Subject subject;
    private final Client client;

    public PiperRestAPIClient(String apiRootUrl, Subject subject) {
        this(ClientBuilder.newClient(new ClientConfig()), apiRootUrl, subject);
    }

    public PiperRestAPIClient(Client client, String apiRootUrl, Subject subject) {
        this.client = client;
        this.apiRootUrl = apiRootUrl;
        this.subject = subject;
    }

    public Map getManagedPipeline(String uuid) {
        return doGetRequest(String.format("%s/api/v1/managed_pipelines/%s", this.apiRootUrl, uuid));
    }

    public String deployPipeline(Object pipeline) {
        return doPostRequest(String.format("%s/api/v1/managed_pipelines", this.apiRootUrl), pipeline);
    }

    public Map getConnections(String type) {
        return doGetRequest(String.format("%s/api/v1/connections/search?page_size=%d&conn_type=%s",
                this.apiRootUrl, DEFAULT_PAGE_SIZE, encodeParam(type)));
    }

    public Map getPipelineRuns(String uuid, Long from, Long to, Integer page, Integer pageSize) {
        // FIXME don't send params if not set
        // FIXME call search
        // FIXME where should date conversion happen
        return doGetRequest(String.format("%s/api/v1/pipelines/%s/runs?page=%d&page_size=%d",
                this.apiRootUrl, uuid, page, pageSize));
    }

    public Map getPipelineState(String uuid, String executionDate) {
        // FIXME only pass execution date if present
        return doGetRequest(String.format("%s/api/v1/pipeline/state?pipeline_id=%s",
                this.apiRootUrl, uuid));
    }

    public Map getTaskGraph(String uuid, String executionDate) {
        return doGetRequest(String.format("%s/api/v1/pipeline/task_graph?pipeline_id=%s&execution_date=%s",
                this.apiRootUrl, uuid, executionDate));
    }

    private String encodeParam(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String doPostRequest(final String requestUrl, final Object bodyObject) {
        try {
            return Subject.doAs(subject, new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return JsonClientUtil.postEntity(client.target(requestUrl), bodyObject, REST_API_MEDIA_TYPE, String.class);
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
            return Subject.doAs(subject, new PrivilegedAction<Map>() {
                @Override
                public Map run() {
                    return JsonClientUtil.getEntity(client.target(requestUrl), REST_API_MEDIA_TYPE, Map.class);
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
}
