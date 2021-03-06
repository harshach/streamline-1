package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.*;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.metrics.athenax.topology.AthenaxTopologyMetricsImpl;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;

import org.jooq.lambda.Unchecked;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.security.Permission.READ;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.OK;


@Path("/v1/catalog/stream")
@Produces(MediaType.APPLICATION_JSON)
public class StreamTopologyViewModeResource {

    public static final int THRESHOLD_VALID_MINIMUM_METRICS_POINTS = 3;
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final TopologyMetricsService metricsService;

    public StreamTopologyViewModeResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService,
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

        Topology topology = catalogService.getTopology(topologyId);
        if (topology == null) {
            throw new EntityNotFoundException("Topology not found topologyId: " + topologyId);
        }

        String asUser = WSUtils.getUserFromSecurityContext(securityContext);

        Map<String, String> metricParams = new HashMap<>();

        // FIXME T2184621 remove hack, need interface updates
        AthenaxTopologyMetricsImpl topologyMetricsService = (AthenaxTopologyMetricsImpl)
                metricsService.getTopologyMetricsInstanceHack(topology, topology.getNamespaceId());

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
                    metricsService.getTopologyStats(topology, from, to, topology.getNamespaceId(), asUser);

            long prevFrom = from - (to - from);
            long prevTo = from - 1;
            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevTopologyMetrics =
                    metricsService.getTopologyStats(topology, prevFrom, prevTo, topology.getNamespaceId(), asUser);

            if (!checkMetricsResponseHasFullRangeOfTime(prevTopologyMetrics, prevFrom, prevTo)) {
                prevTopologyMetrics = null;
            }

            ComponentMetricSummary viewModeComponentMetric = ComponentMetricSummary.convertStreamingComponentMetric(
                    topologyMetrics, prevTopologyMetrics);
            TopologyWithMetric metric = new TopologyWithMetric(topology, viewModeComponentMetric,
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
    @Path("/topologies/{topologyId}/sources/metrics")
    @Timed
    public Response listSources(@PathParam("topologyId") Long topologyId,
                                @QueryParam("from") Long from,
                                @QueryParam("to") Long to,
                                @Context UriInfo uriInfo,
                                @Context SecurityContext securityContext) throws IOException {
        return listComponents(topologyId, from, to, uriInfo, securityContext, TopologySource.class);
    }

    @GET
    @Path("/topologies/{topologyId}/sources/{sourceId}/metrics")
    @Timed
    public Response getSource(@PathParam("topologyId") Long topologyId,
                              @PathParam("sourceId") Long sourceId,
                              @QueryParam("from") Long from,
                              @QueryParam("to") Long to,
                              @Context UriInfo uriInfo,
                              @Context SecurityContext securityContext) throws IOException {
        return getComponent(topologyId, sourceId, from, to, uriInfo, securityContext, TopologySource.class);
    }

    @GET
    @Path("/topologies/{topologyId}/processors/metrics")
    @Timed
    public Response listProcessors(@PathParam("topologyId") Long topologyId,
                                   @QueryParam("from") Long from,
                                   @QueryParam("to") Long to,
                                   @Context UriInfo uriInfo,
                                   @Context SecurityContext securityContext) throws IOException {
        return listComponents(topologyId, from, to, uriInfo, securityContext, TopologyProcessor.class);
    }

    @GET
    @Path("/topologies/{topologyId}/processors/{processorId}/metrics")
    @Timed
    public Response getProcessors(@PathParam("topologyId") Long topologyId,
                                  @PathParam("processorId") Long processorId,
                                  @QueryParam("from") Long from,
                                  @QueryParam("to") Long to,
                                  @Context UriInfo uriInfo,
                                  @Context SecurityContext securityContext) throws IOException {
        return getComponent(topologyId, processorId, from, to, uriInfo, securityContext, TopologyProcessor.class);
    }

    @GET
    @Path("/topologies/{topologyId}/sinks/metrics")
    @Timed
    public Response listSinks(@PathParam("topologyId") Long topologyId,
                              @QueryParam("from") Long from,
                              @QueryParam("to") Long to,
                              @Context UriInfo uriInfo,
                              @Context SecurityContext securityContext) throws IOException {
        return listComponents(topologyId, from, to, uriInfo, securityContext, TopologySink.class);
    }

    @GET
    @Path("/topologies/{topologyId}/sinks/{sinkId}/metrics")
    @Timed
    public Response getSink(@PathParam("topologyId") Long topologyId,
                            @PathParam("sinkId") Long sinkId,
                            @QueryParam("from") Long from,
                            @QueryParam("to") Long to,
                            @Context UriInfo uriInfo,
                            @Context SecurityContext securityContext) throws IOException {
        return getComponent(topologyId, sinkId, from, to, uriInfo, securityContext, TopologySink.class);
    }

    private Response listComponents(Long topologyId, Long from, Long to, UriInfo uriInfo,
                                    SecurityContext securityContext, Class<? extends TopologyComponent> clazz) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);

        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);

        List<com.hortonworks.streamline.common.QueryParam> queryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo);

        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            Collection<? extends TopologyComponent> components;
            if (clazz.equals(TopologySource.class)) {
                components = catalogService.listTopologySources(queryParams);
            } else if (clazz.equals(TopologyProcessor.class)) {
                components = catalogService.listTopologyProcessors(queryParams);
            } else if (clazz.equals(TopologySink.class)) {
                components = catalogService.listTopologySinks(queryParams);
            } else if (clazz.equals(TopologyTask.class)) {
                components = catalogService.listTopologyTasks(queryParams);
            } else {
                throw new IllegalArgumentException("Unexpected class in parameter: " + clazz);
            }
            if (components != null) {
                String asUser = WSUtils.getUserFromSecurityContext(securityContext);

                List<TopologyComponentWithMetric> componentsWithMetrics = components.stream()
                        .map(Unchecked.function(s -> {
                            ComponentMetricSummary overviewMetric;
                            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric currentMetric =
                                    metricsService.getComponentStats(topology, s, from, to, topology.getNamespaceId(), asUser);
                            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric previousMetric =
                                    metricsService.getComponentStats(topology, s, from - (to - from), from - 1, topology.getNamespaceId(), asUser);
                            if (clazz.equals(TopologySource.class)) {
                                overviewMetric = ComponentMetricSummary.convertStreamingComponentMetric(
                                        currentMetric, previousMetric);
                            } else {
                                overviewMetric = ComponentMetricSummary.convertStreamingComponentMetric(
                                        currentMetric, previousMetric);
                            }

                            return new TopologyComponentWithMetric(s, overviewMetric, currentMetric);
                        }))
                        .collect(toList());

                return WSUtils.respondEntities(componentsWithMetrics, OK);
            }

            throw EntityNotFoundException.byFilter(queryParams.toString());
        }

        throw EntityNotFoundException.byName("topology ID " + topologyId);
    }

    private Response getComponent(Long topologyId, Long componentId, Long from, Long to, UriInfo uriInfo,
                                  SecurityContext securityContext, Class<? extends TopologyComponent> clazz)
            throws IOException {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            TopologyComponent component;
            if (clazz.equals(TopologySource.class)) {
                component = catalogService.getTopologySource(topologyId, componentId);
            } else if (clazz.equals(TopologyProcessor.class)) {
                component = catalogService.getTopologyProcessor(topologyId, componentId);
            } else if (clazz.equals(TopologySink.class)) {
                component = catalogService.getTopologySink(topologyId, componentId);
            } else {
                throw new IllegalArgumentException("Unexpected class in parameter: " + clazz);
            }

            if (component != null) {
                String asUser = WSUtils.getUserFromSecurityContext(securityContext);

                ComponentMetricSummary overviewMetric;
                TopologyTimeSeriesMetrics.TimeSeriesComponentMetric currentMetric =
                        metricsService.getComponentStats(topology, component, from, to, topology.getNamespaceId(), asUser);
                TopologyTimeSeriesMetrics.TimeSeriesComponentMetric previousMetric =
                        metricsService.getComponentStats(topology, component, from - (to - from), from - 1, topology.getNamespaceId(), asUser);
                if (clazz.equals(TopologySource.class)) {
                    overviewMetric = ComponentMetricSummary.convertStreamingComponentMetric(currentMetric, previousMetric);
                } else if (clazz.equals(TopologyProcessor.class) || clazz.equals(TopologySink.class)) {
                    overviewMetric = ComponentMetricSummary.convertStreamingComponentMetric(currentMetric, previousMetric);
                } else {
                    overviewMetric = null;
                }

                TopologyComponentWithMetric componentWithMetrics =
                        new TopologyComponentWithMetric(component, overviewMetric, currentMetric);

                return WSUtils.respondEntity(componentWithMetrics, OK);
            }

            throw EntityNotFoundException.byName("component ID " + componentId);
        }

        throw EntityNotFoundException.byName("topology ID " + topologyId);
    }

    private void assertTimeRange(Long from, Long to) {
        if (from == null) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException.missingParameter("from");
        }
        if (to == null) {
            throw BadRequestException.missingParameter("to");
        }
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
