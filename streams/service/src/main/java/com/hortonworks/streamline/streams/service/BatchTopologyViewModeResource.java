package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.*;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.metrics.piper.topology.PiperTopologyMetricsImpl;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.security.Permission.READ;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.OK;



@Path("/v1/catalog/batch")
@Produces(MediaType.APPLICATION_JSON)
public class BatchTopologyViewModeResource {

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final TopologyMetricsService metricsService;

    private static final String PARAM_TO = "to";
    private static final String PARAM_FROM = "from";
    private static final String PARAM_METRIC_KEY_NAME = "metricKeyName";
    private static final String PARAM_METRIC_QUERY = "metricQuery";


    public BatchTopologyViewModeResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService,
                                         TopologyMetricsService metricsService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.metricsService = metricsService;
    }


    @GET
    @Path("/topologies/{topologyId}/metrics/{metricKeyName}")
    @Timed
    public Response getTopologyTimeSeriesMetrics(@PathParam("topologyId") Long topologyId,
                                                 @PathParam("metricKeyName") String metricKeyName,
                                                 @QueryParam("metricQuery") String metricQuery,
                                                 @QueryParam("from") Long from,
                                                 @QueryParam("to") Long to,
                                                 @QueryParam("namespaceId") Long namespaceId,
                                                 @Context UriInfo uriInfo,
                                                 @Context SecurityContext securityContext) throws IOException {

        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);
        assertRequired(metricQuery, "metricQuery,");

        Topology topology = catalogService.getTopology(topologyId);
        if (topology == null) {
            throw new EntityNotFoundException("Topology not found topologyId: " + topologyId);
        }

        String asUser = WSUtils.getUserFromSecurityContext(securityContext);

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        List<String> ignore = Arrays.asList(PARAM_TO, PARAM_FROM, PARAM_METRIC_KEY_NAME, PARAM_METRIC_QUERY);

        Map<String, String> metricParams = toSingleValueMap(queryParams, ignore);

        Map<Long, Map<Long, Double>> topologyMetrics = metricsService.getComponentTimeSeriesMetrics(
                topology, namespaceId, metricKeyName, metricParams, from, to, asUser);

        return WSUtils.respondEntity(topologyMetrics, OK);
    }


    @GET
    @Path("/topologies/{topologyId}/metrics")
    @Timed
    public Response getTopology(@PathParam("topologyId") Long topologyId,
                                @QueryParam("from") Long from,
                                @QueryParam("to") Long to,
                                @QueryParam("namespaceId") Long namespaceId,
                                @Context SecurityContext securityContext) throws IOException {


        throw new UnsupportedOperationException("/v1/catalog/batch/topologies/{topologyId}/metrics endpoint not implemented");
    }


    @GET
    @Path("/topologies/{topologyId}/executions")
    @Timed
    public Response getExecutions(@PathParam("topologyId") Long topologyId,
                                  @QueryParam("from") Long from,
                                  @QueryParam("to") Long to,
                                  @DefaultValue("0") @QueryParam("page") Integer page,
                                  @DefaultValue("20") @QueryParam("pageSize") Integer pageSize,
                                  @QueryParam("namespaceId") Long namespaceId,
                                  @Context UriInfo uriInfo,
                                  @Context SecurityContext securityContext) throws IOException {

        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(topologyId);

        if (topology == null) {
            throw new EntityNotFoundException("Topology not found topologyId: " + topologyId);
        }

        String asUser = WSUtils.getUserFromSecurityContext(securityContext);

        Map executions = metricsService.getExecutions(topology, namespaceId, from, to, page, pageSize, asUser);

        return WSUtils.respondEntity(executions, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/executions/{executionDate}")
    @Timed
    public Response getExecution(@PathParam("topologyId") Long topologyId,
                                 @PathParam("executionDate") String executionDate,
                                 @QueryParam("namespaceId") Long namespaceId,
                                 @Context UriInfo uriInfo,
                                 @Context SecurityContext securityContext) throws IOException {

        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        Topology topology = catalogService.getTopology(topologyId);

        if (topology == null) {
            throw new EntityNotFoundException("Topology not found topologyId: " + topologyId);
        }

        String asUser = WSUtils.getUserFromSecurityContext(securityContext);

        Map execution = metricsService.getExecution(topology, namespaceId, executionDate, asUser);

        return WSUtils.respondEntity(execution, OK);

    }


    private void assertTimeRange(Long from, Long to) {
        if (from == null) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException.missingParameter("from");
        }
        if (to == null) {
            throw BadRequestException.missingParameter("to");
        }
    }

    private void assertRequired(Object object, String value) {
        if ( object == null) {
            throw BadRequestException.missingParameter(value);
        }
    }

    private Map<String, String> toSingleValueMap(MultivaluedMap<String, String> multivaluedMap, List<String> ignore) {
        Map<String, String> result = new HashMap<>();
        for (String key : multivaluedMap.keySet()) {
            if (ignore != null && !ignore.contains(key)) {
                result.put(key, multivaluedMap.getFirst(key));
            }
        }
        return result;
    }
}
