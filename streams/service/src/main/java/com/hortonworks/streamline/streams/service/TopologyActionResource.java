/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.request.TopologyAlreadyExistsOnCluster;
import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.layout.component.ValidationStatus;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import io.dropwizard.jersey.sessions.Session;

import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static com.hortonworks.streamline.streams.catalog.Topology.NAMESPACE;
import static com.hortonworks.streamline.streams.security.Permission.EXECUTE;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.security.SecurityUtil.getAllUserGroups;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyActionResource {

    private static final int FORK_JOIN_POOL_PARALLELISM = 10;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final TopologyActionsService actionsService;

    public TopologyActionResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService,
                                  TopologyActionsService actionsService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.actionsService = actionsService;
    }

    @GET
    @Path("/topologies/{topologyId}/actions/status")
    @Timed
    public Response topologyStatus (@PathParam("topologyId") Long topologyId,
                                    @Context SecurityContext securityContext,
                                    @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                userGroups, NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            List<TopologyActions.Status> statuses = actionsService.topologyStatus(result, asUser);
            Response response;
            if (statuses != null) {
                response = WSUtils.respondEntities(statuses, OK);
            } else {
                response = WSUtils.respondEntities(Collections.emptyList(), OK);
            }
            return response;
        }

        throw EntityNotFoundException.byMessage("There is no workflow with %s".format(topologyId.toString()));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/status")
    @Timed
    public Response topologyStatusVersion(@PathParam("topologyId") Long topologyId,
                                          @PathParam("versionId") Long versionId,
                                          @Context SecurityContext securityContext,
                                          @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                userGroups, NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            List<TopologyActions.Status> statuses = actionsService.topologyStatus(result, asUser);
            Response response;
            if (statuses != null) {
                response =  WSUtils.respondEntities(statuses, OK);
            } else {
                response = WSUtils.respondEntities(Collections.emptyList(), OK);
            }
            return response;
        }

        throw EntityNotFoundException.byMessage("There is no workflow with %s version %s".format(topologyId.toString(), versionId.toString()));
    }

    @POST
    @Path("/topologies/{topologyId}/actions/validate")
    @Timed
    public Response validateTopology (@PathParam("topologyId") Long topologyId, @Context SecurityContext securityContext, @Session HttpSession httpSession) {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            //catalogService.validateTopology(SCHEMA, topologyId);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/validate")
    @Timed
    public Response validateTopologyVersion(@PathParam("topologyId") Long topologyId,
                                            @PathParam("versionId") Long versionId, @Context SecurityContext securityContext, @Session HttpSession httpSession) {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            List<ValidationStatus> statuses = catalogService.validateUserInput(result);
            Response response;
            if (statuses != null) {
                response =  WSUtils.respondEntities(statuses, OK);
            } else {
                response = WSUtils.respondEntities(Collections.emptyList(), OK);
            }

            return response;
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/deploy")
    @Timed
    public Response deployTopology (@PathParam("topologyId") Long topologyId, @Context SecurityContext securityContext, @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            return deploy(topology, securityContext);
        }
        throw EntityNotFoundException.byId(topologyId.toString());
    }


    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/deploy")
    @Timed
    public Response deployTopologyVersion(@PathParam("topologyId") Long topologyId,
                                          @PathParam("versionId") Long versionId,
                                          @Context SecurityContext securityContext,
                                          @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId, versionId);
        if (topology != null) {
            return deploy(topology, securityContext);
        }
        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    private Response deploy(Topology topology, SecurityContext securityContext) {
        String asUser = WSUtils.getUserFromSecurityContext(securityContext);
        try {
            ParallelStreamUtil.runAsync(() -> actionsService.deployTopology(topology, asUser), forkJoinPool);
            return WSUtils.respondEntity(topology, OK);
        } catch (TopologyAlreadyExistsOnCluster ex) {
            return ex.getResponse();
        }
    }

    @POST
    @Path("/topologies/{topologyId}/actions/kill")
    @Timed
    public Response killTopology (@PathParam("topologyId") Long topologyId, @Context SecurityContext securityContext, @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            actionsService.killTopology(result, WSUtils.getUserFromSecurityContext(securityContext));
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/kill")
    @Timed
    public Response killTopologyVersion(@PathParam("topologyId") Long topologyId,
                                        @PathParam("versionId") Long versionId,
                                        @Context SecurityContext securityContext,
                                        @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            actionsService.killTopology(result, WSUtils.getUserFromSecurityContext(securityContext));
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/suspend")
    @Timed
    public Response suspendTopology (@PathParam("topologyId") Long topologyId, @Context SecurityContext securityContext, @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            actionsService.suspendTopology(result, WSUtils.getUserFromSecurityContext(securityContext));
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/suspend")
    @Timed
    public Response suspendTopologyVersion(@PathParam("topologyId") Long topologyId,
                                           @PathParam("versionId") Long versionId,
                                           @Context SecurityContext securityContext,
                                           @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            actionsService.suspendTopology(result, WSUtils.getUserFromSecurityContext(securityContext));
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/resume")
    @Timed
    public Response resumeTopology (@PathParam("topologyId") Long topologyId,
                                    @Context SecurityContext securityContext,
                                    @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            actionsService.resumeTopology(result, WSUtils.getUserFromSecurityContext(securityContext));
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/resume")
    @Timed
    public Response resumeTopologyVersion(@PathParam("topologyId") Long topologyId,
                                          @PathParam("versionId") Long versionId,
                                          @Context SecurityContext securityContext,
                                          @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            actionsService.resumeTopology(result, WSUtils.getUserFromSecurityContext(securityContext));
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

}
