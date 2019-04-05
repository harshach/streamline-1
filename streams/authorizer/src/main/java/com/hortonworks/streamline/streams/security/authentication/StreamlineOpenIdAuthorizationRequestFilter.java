package com.hortonworks.streamline.streams.security.authentication;

import com.google.common.base.Strings;
import com.hortonworks.streamline.common.exception.service.exception.request.WebserviceAuthorizationException;
import com.hortonworks.streamline.streams.security.StreamlinePrincipal;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import io.dropwizard.jersey.sessions.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(100)
public class StreamlineOpenIdAuthorizationRequestFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory
        .getLogger(MethodHandles.lookup().lookupClass());
    private static final String X_AUTH_PARAMS_EMAIL_HEADER = "X-Auth-Params-Email";
    private static final String X_UBER_SOURCE_HEADER = "X-Uber-Source";
    private static final String HEALTH_AGENT = "health-agent";
    private static final String X_AUTH_PARAMS_GROUPS_HEADER = "X-Auth-Params-Groups";
    public  static final String USER_GROUPS = "User-Groups";

    @Context
    private HttpServletRequest httpRequest;

    public void filter(ContainerRequestContext containerRequestContext) {
        MultivaluedMap<String, String> headers = containerRequestContext.getHeaders();
        if (isHealthEndpoint(headers)) {
            LOG.debug("Caller is health-agent, no authorization needed.");
            return;
        }

        String authorizedUserName = extractAuthorizedUserName(headers);
        LOG.debug("AuthorizedUserName:{}", authorizedUserName);
        StreamlinePrincipal streamlinePrincipal = new StreamlinePrincipal(authorizedUserName);
        String scheme = containerRequestContext.getUriInfo().getRequestUri().getScheme();
        StreamlineSecurityContext streamlineSecurityContext = new StreamlineSecurityContext(streamlinePrincipal, scheme, StreamlineSecurityContext.OPENID_AUTH);
        LOG.debug("SecurityContext {}", streamlineSecurityContext);
        containerRequestContext.setSecurityContext(streamlineSecurityContext);
        if (this.httpRequest.getSession().isNew()) {
            this.httpRequest.getSession().setAttribute(USER_GROUPS, extractUserGroups(headers));
        }
    }

    private boolean isHealthEndpoint(MultivaluedMap<String, String> headers) {
        String uberSource = headers.getFirst(X_UBER_SOURCE_HEADER);
        return HEALTH_AGENT.equalsIgnoreCase(uberSource);
    }

    private String extractAuthorizedUserName(MultivaluedMap<String, String> headers) {
        LOG.debug("Request Headers:{}", headers);
        String openIdEmail = headers.getFirst(X_AUTH_PARAMS_EMAIL_HEADER);
        if (Strings.isNullOrEmpty(openIdEmail)) {
            throw new WebserviceAuthorizationException("Not authorized");
        }
        String[] openIdEmailParts = openIdEmail.split("@");
        return openIdEmailParts[0];
    }

    private Set<String> extractUserGroups(MultivaluedMap<String, String> headers) {
        LOG.debug("Request Headers:{}", headers);
        String headerUserGroups = headers.getFirst(X_AUTH_PARAMS_GROUPS_HEADER);
        if (Strings.isNullOrEmpty(headerUserGroups)) {
            return Collections.emptySet();
        }
        String[] userGroups = headerUserGroups.split(",");
        Set<String> groups = new HashSet<>(Arrays.asList(userGroups));
        return groups;
    }
}
