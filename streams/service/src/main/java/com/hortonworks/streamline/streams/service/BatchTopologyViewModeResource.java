package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.*;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.metrics.piper.m3.M3MetricsWithPiperQuerier;
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

    public static final int THRESHOLD_VALID_MINIMUM_METRICS_POINTS = 3;
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
    public Response getTopologyMetrics(@PathParam("topologyId") Long topologyId,
                                       @PathParam("metricKeyName") String metricKeyName,
                                       @QueryParam("metricQuery") String metricQuery,
                                       @QueryParam("from") Long from,
                                       @QueryParam("to") Long to,
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

        // FIXME T2184621 remove hack, need interface updates
        PiperTopologyMetricsImpl topologyMetricsService = (PiperTopologyMetricsImpl)
                metricsService.getTopologyMetricsInstanceHack(topology);

        Map<Long, Object> topologyMetrics = topologyMetricsService.getTimeSeriesMetrics(
                topology, metricKeyName, metricQuery, metricParams, from, to, asUser);

        return WSUtils.respondEntity(topologyMetrics, OK);

    }


    @GET
    @Path("/topologies/{topologyId}/metrics")
    @Timed
    public Response getTopology(@PathParam("topologyId") Long topologyId,
                                @QueryParam("from") Long from,
                                @QueryParam("to") Long to,
                                @Context SecurityContext securityContext) throws IOException {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric topologyMetrics =
                    metricsService.getTopologyStats(topology, from, to, asUser);

            long prevFrom = from - (to - from);
            long prevTo = from - 1;
            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevTopologyMetrics =
                    metricsService.getTopologyStats(topology, prevFrom, prevTo, asUser);

            if (!checkMetricsResponseHasFullRangeOfTime(prevTopologyMetrics, prevFrom, prevTo)) {
                prevTopologyMetrics = null;
            }

            ComponentMetricSummary viewModeComponentMetric = ComponentMetricSummary.convertStreamingComponentMetric(
                    topologyMetrics, prevTopologyMetrics);
            com.hortonworks.streamline.streams.service.BatchTopologyViewModeResource.TopologyWithMetric metric = new com.hortonworks.streamline.streams.service.BatchTopologyViewModeResource.TopologyWithMetric(topology, viewModeComponentMetric,
                    topologyMetrics);
            return WSUtils.respondEntity(metric, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    private boolean checkMetricsResponseHasFullRangeOfTime(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                           long from, long to) {
        if (metrics == null) {
            return false;
        }

        Map<Long, Double> target = metrics.getMetrics().get("processTime");
        if (target == null || target.size() == 0) {
            // fail back to see output records
            target = metrics.getMetrics().get("outputRecords");

            if (target == null || target.size() == 0) {
                return false;
            }
        }

        List<Long> sortedTimestamp = target.keySet().stream().sorted().collect(toList());
        if (sortedTimestamp.size() < THRESHOLD_VALID_MINIMUM_METRICS_POINTS) {
            // no granularity to check, or not enough values to use
            return false;
        }

        Long firstTimestamp = sortedTimestamp.get(0);
        Long secondTimestamp = sortedTimestamp.get(1);
        Long lastTimestamp = sortedTimestamp.get(sortedTimestamp.size() - 1);

        long granularity = secondTimestamp - firstTimestamp;

        // assuming that time-series DB will provide the self-aggregated metrics points
        // only with ranges which raw points are available

        // this means time-series DB doesn't have metric points in earlier part of time range
        if (firstTimestamp - from > granularity) {
            return false;
        }

        // this means time-series DB doesn't have metric points in later part of time range
        if (to - lastTimestamp > granularity) {
            return false;
        }

        return true;
    }


    @GET
    @Path("/topologies/{topologyId}/executions")
    @Timed
    public Response getExecutions(@PathParam("topologyId") Long topologyId,
                                  @QueryParam("from") Long from,
                                  @QueryParam("to") Long to,
                                  @DefaultValue("0") @QueryParam("page") Integer page,
                                  @DefaultValue("20") @QueryParam("pageSize") Integer pageSize,
                                  @Context UriInfo uriInfo,
                                  @Context SecurityContext securityContext) throws IOException {

        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(topologyId);

        if (topology == null) {
            throw new EntityNotFoundException("Topology not found topologyId: " + topologyId);
        }

        // FIXME T2184621 remove hack, need interface updates
        PiperTopologyMetricsImpl topologyMetrics = (PiperTopologyMetricsImpl)
                metricsService.getTopologyMetricsInstanceHack(topology);

        Map executions = topologyMetrics.getExecutions(
                CatalogToLayoutConverter.getTopologyLayout(topology), from, to, page, pageSize);

        return WSUtils.respondEntity(executions, OK);

    }

    @GET
    @Path("/topologies/{topologyId}/executions/{executionDate}")
    @Timed
    public Response getExecution(@PathParam("topologyId") Long topologyId,
                                 @PathParam("executionDate") String executionDate,
                                 @Context UriInfo uriInfo,
                                 @Context SecurityContext securityContext) throws IOException {

        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        Topology topology = catalogService.getTopology(topologyId);

        if (topology == null) {
            throw new EntityNotFoundException("Topology not found topologyId: " + topologyId);
        }

        // FIXME T2184621 remove hack, need interface updates
        PiperTopologyMetricsImpl topologyMetrics = (PiperTopologyMetricsImpl)
                metricsService.getTopologyMetricsInstanceHack(topology);

        Map execution = topologyMetrics.getExecution(
                            CatalogToLayoutConverter.getTopologyLayout(topology),
                            executionDate);

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

    private static class TopologyWithMetric {
        private final Topology topology;
        private final ComponentMetricSummary overviewMetrics;
        private final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric timeSeriesMetrics;

        public TopologyWithMetric(Topology topology, ComponentMetricSummary overviewMetrics,
                                  TopologyTimeSeriesMetrics.TimeSeriesComponentMetric timeSeriesMetrics) {
            this.topology = topology;
            this.overviewMetrics = overviewMetrics;
            this.timeSeriesMetrics = timeSeriesMetrics;
        }

        public Topology getTopology() {
            return topology;
        }

        public ComponentMetricSummary getOverviewMetrics() {
            return overviewMetrics;
        }

        public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTimeSeriesMetrics() {
            return timeSeriesMetrics;
        }
    }

    private static class TopologyComponentWithMetric {
        private final TopologyComponent component;
        private final ComponentMetricSummary overviewMetrics;
        private final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric timeSeriesMetrics;

        public TopologyComponentWithMetric(TopologyComponent component,
                                           ComponentMetricSummary overviewMetrics,
                                           TopologyTimeSeriesMetrics.TimeSeriesComponentMetric timeSeriesMetrics) {
            this.component = component;
            this.overviewMetrics = overviewMetrics;
            this.timeSeriesMetrics = timeSeriesMetrics;
        }

        public TopologyComponent getComponent() {
            return component;
        }

        public ComponentMetricSummary getOverviewMetrics() {
            return overviewMetrics;
        }

        public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTimeSeriesMetrics() {
            return timeSeriesMetrics;
        }
    }
}
