package com.hortonworks.streamline.streams.metrics.topology.client;

import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.WrappedWebApplicationException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.piper.common.PiperConstants.X_UBER_ORIGIN;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.X_UBER_SOURCE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.UWORC_UBER_SERVICE_NAME;

public class M3RestAPIClient {

    private static final Logger LOG = LoggerFactory.getLogger(M3RestAPIClient.class);

    private static final String TARGET = "target";
    private static final String FORMAT = "format";
    private static final String FORMAT_JSON = "json";
    private static final String FROM = "from";
    private static final String UNTIL = "until";
    private static final String M3QL_RENDER_ENDPOINT = "/m3ql/render";


    private final URI apiRootUrl;
    private final Subject subject;
    private final Client client;

    public M3RestAPIClient(String apiRootUrl, Subject subject) throws URISyntaxException {
        this(ClientBuilder.newClient(new ClientConfig()), apiRootUrl, subject);
    }

    public M3RestAPIClient(Client client, String apiRootUrl, Subject subject) throws URISyntaxException {
        this.client = client;
        client.register(new LoggingFilter());
        this.apiRootUrl = new URI(apiRootUrl);
        this.subject = subject;
    }

    public List<Map> getMetrics(String query, long from, long until, String asUser) {
        JerseyUriBuilder uriBuilder = new JerseyUriBuilder();
        URI requestUrl = uriBuilder.uri(this.apiRootUrl)
                .path(M3QL_RENDER_ENDPOINT)
                .queryParam(TARGET, query)
                .queryParam(FORMAT, FORMAT_JSON)
                .queryParam(FROM, from)
                .queryParam(UNTIL, until)
                .build();
        return doGetRequest(requestUrl , asUser);
    }

    private List<Map> doGetRequest(final URI requestUrl, String asUser) {
        try {
            Map<String, String> headers = new HashMap<>();
            if (asUser == null || asUser.trim().isEmpty()) {
                String msg = "User empty, setting %s to %s instead of user";
                LOG.warn(String.format(msg, X_UBER_SOURCE, UWORC_UBER_SERVICE_NAME));
                asUser = UWORC_UBER_SERVICE_NAME;
            }
            headers.put(X_UBER_ORIGIN, UWORC_UBER_SERVICE_NAME);
            headers.put(X_UBER_SOURCE, asUser);

            LOG.debug("GET request to M3: " + requestUrl);
            return Subject.doAs(subject, new PrivilegedAction<List>() {
                @Override
                public List run() {
                    List<Map> entities =
                            JsonClientUtil.getEntities(client.target(requestUrl), headers, Map.class);
                    return entities;

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
