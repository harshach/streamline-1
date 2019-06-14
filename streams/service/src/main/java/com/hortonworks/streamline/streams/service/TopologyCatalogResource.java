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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.*;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.topology.TopologyData;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.catalog.OwnerGroup;
import com.hortonworks.streamline.streams.security.catalog.User;
import com.hortonworks.streamline.streams.security.service.SecurityCatalogService;
import com.hortonworks.streamline.streams.security.catalog.AclEntry;

import io.dropwizard.jersey.sessions.Session;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.hortonworks.streamline.streams.actions.piper.topology.ManagedPipelineGenerator.PIPER_TOPOLOGY_CONFIG_OWNER;
import static com.hortonworks.streamline.streams.catalog.Topology.NAMESPACE;
import static com.hortonworks.streamline.streams.catalog.TopologyVersion.VERSION_PREFIX;
import static com.hortonworks.streamline.streams.security.Permission.DELETE;
import static com.hortonworks.streamline.streams.security.Permission.EXECUTE;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.security.Permission.WRITE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;


@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyCatalogResource.class);

    private final String urlApplication = "%s://%s/#/projects/%d/applications/%d/view";
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final TopologyActionsService actionsService;
    private final SecurityCatalogService securityCatalogService;

    private final static int OFFSET = 0;
    private final static int LIMIT  = 0;


    public TopologyCatalogResource(StreamlineAuthorizer authorizer,
                                   StreamCatalogService catalogService,
                                   TopologyActionsService actionsService,
                                   SecurityCatalogService securityCatalogService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.actionsService = actionsService;
        this.securityCatalogService = securityCatalogService;
    }

    @GET
    @Path("/applications/{applicationId}")
    @Timed
    public Response getApplicationStatus (@PathParam("applicationId") String applicationId,
                                          @Context SecurityContext securityContext,
                                          @Context UriInfo uriInfo,
                                          @Session HttpSession httpSession) throws Exception {
        TopologyRuntimeIdMap topologyRuntimeIdMap = catalogService.getTopologyRuntimeIdMap(applicationId);
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyRuntimeIdMap.getTopologyId(), READ, EXECUTE);
        Topology result = catalogService.getTopology(topologyRuntimeIdMap.getTopologyId());
        if (result != null) {
            Long projectId  = result.getProjectId();
            String url = String.format(urlApplication, uriInfo.getBaseUri().getScheme(),uriInfo.getBaseUri().getAuthority(), projectId, topologyRuntimeIdMap.getTopologyId());
            return Response.seeOther(new URI(url)).build();
        }

        throw EntityNotFoundException.byId(applicationId);
    }


    @GET
    @Path("/system/engines")
    @Timed
    public Response listEngines(@Context SecurityContext securityContext) {
        Collection<Engine> engines = catalogService.listEngines();
        boolean topologyUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER);
        if (topologyUser) {
            LOG.debug("Returning all engines since user has role: {}", Roles.ROLE_TOPOLOGY_USER);
        } else {
            engines = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, engines, READ);
        }

        Response response;
        if (engines != null) {
            response = WSUtils.respondEntities(engines, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }

        return response;
    }


    @POST
    @Path("/system/engines")
    @Timed
    public Response addEngine(Engine engine, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        if (StringUtils.isEmpty(engine.getName())) {
            throw BadRequestException.missingParameter(Engine.NAME);
        }
        Engine createdEngine = catalogService.addEngine(engine);
        SecurityUtil.addAcl(authorizer, securityContext, Engine.NAMESPACE, engine.getId(),
                            EnumSet.allOf(Permission.class));
        return WSUtils.respondEntity(createdEngine, CREATED);
    }


    @GET
    @Path("/system/engines/{engineId}/templates")
    @Timed
    public Response listTemplates(@PathParam("engineId") Long engineId,
                                @Context SecurityContext securityContext) {
        Collection<Template> templates = catalogService.listTemplates(
                com.hortonworks.streamline.common.QueryParam.params(Template.ENGINEID, engineId.toString()));
        boolean topologyUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER);
        if (topologyUser) {
            LOG.debug("Returning all templates since user has role: {}", Roles.ROLE_TOPOLOGY_USER);
        } else {
            templates = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, templates, READ);
        }

        Response response;
        if (templates != null) {
            response = WSUtils.respondEntities(templates, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }

        return response;
    }

    @GET
    @Path("/system/engines/templates")
    @Timed
    public Response listTemplates(@Context SecurityContext securityContext) {
        Collection<Template> templates = catalogService.listTemplates(new ArrayList<>());
        boolean topologyUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER);
        if (topologyUser) {
            LOG.debug("Returning all templates since user has role: {}", Roles.ROLE_TOPOLOGY_USER);
        } else {
            templates = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, templates, READ);
        }

        Response response;
        if (templates != null) {
            response = WSUtils.respondEntities(templates, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }

        return response;
    }

    @POST
    @Path("/system/engines/{engineName}/templates")
    @Timed
    public Response addTemplate(@PathParam("engineName") String engineName,
                                Template template, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        if (StringUtils.isEmpty(template.getName())) {
            throw BadRequestException.missingParameter(Template.NAME);
        }
        Engine engine = catalogService.getEngine(engineName);
        if (engine != null) {
            template.setEngineId(engine.getId());
            Template createdTemplate = catalogService.addTemplate(template);
            SecurityUtil.addAcl(authorizer, securityContext, Template.NAMESPACE, createdTemplate.getId(),
                    EnumSet.allOf(Permission.class));
            return WSUtils.respondEntity(createdTemplate, CREATED);
        }

        throw EntityNotFoundException.byMessage(String.format("Engine %s Not found", engineName));
    }


    @GET
    @Path("/system/engines/metrics")
    @Timed
    public Response listEngineTemplateMetricsBundles(@Context SecurityContext securityContext) {
        Collection<EngineTemplateMetricsBundle> engineTemplateMetricsBundles = catalogService.listEngineTemplateMetricsBundles();
        boolean topologyUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER);
        if (topologyUser) {
            LOG.debug("Returning all projects since user has role: {}", Roles.ROLE_TOPOLOGY_USER);
        } else {
            engineTemplateMetricsBundles = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, engineTemplateMetricsBundles, READ);
        }

        Response response;
        if (engineTemplateMetricsBundles != null) {
            response = WSUtils.respondEntities(engineTemplateMetricsBundles, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }

        return response;
    }


    @POST
    @Path("/system/engines/metrics")
    @Timed
    public Response addEngineTemplateMetricsBundle(EngineTemplateMetricsBundle engineTemplateMetricsBundle, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        if (StringUtils.isEmpty(engineTemplateMetricsBundle.getName())) {
            throw BadRequestException.missingParameter(EngineTemplateMetricsBundle.NAME);
        }
        EngineTemplateMetricsBundle createdBundle = catalogService.addEngineTemplateMetricsBundle(engineTemplateMetricsBundle);
        SecurityUtil.addAcl(authorizer, securityContext, EngineTemplateMetricsBundle.NAME_SPACE, createdBundle.getId(),
                EnumSet.allOf(Permission.class));
        return WSUtils.respondEntity(createdBundle, CREATED);
    }


    @GET
    @Path("/projects")
    @Timed
    public Response listProjects (@Context SecurityContext securityContext,
                                  @javax.ws.rs.QueryParam("sharedByOther") boolean sharedByOther) {
        boolean adminUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_ADMIN);
        Collection<Project>  projects = new ArrayList<>();
        if (adminUser) {
            projects = catalogService.listProjects();
            LOG.debug("Returning all projects since user has role: {}", Roles.ROLE_ADMIN);
        } else {
            String userName = SecurityUtil.getUserName(securityContext.getUserPrincipal().getName());
            User user = securityCatalogService.getUser(userName);
            List<Long> listProjectIds;
            if (sharedByOther == true) {
                listProjectIds = securityCatalogService.listObjectIdSharedByOthers(user.getId(), Project.NAMESPACE);
            } else {
                listProjectIds = securityCatalogService.listOjectIdByOwner(user.getId(), Project.NAMESPACE);
            }
            for (long projectId: listProjectIds) {
                if (projectId == StreamCatalogService.PLACEHOLDER_ID) {
                    continue;
                }
                Project project = catalogService.getProject(projectId);
                if (project != null) projects.add(project);
            }
        }
        Response response;
        if (projects != null) {
            response = WSUtils.respondEntities(projects, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }

        return response;
    }


    @GET
    @Path("/projects/{projectId}")
    @Timed
    public Response getProjectById(@PathParam("projectId") Long projectId,
                                    @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, projectId, READ);

        Project result = catalogService.getProjectInfo(projectId);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(projectId.toString());
    }

    @POST
    @Path("/projects")
    @Timed
    public Response addProject(Project project, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        if (StringUtils.isEmpty(project.getName())) {
            throw BadRequestException.missingParameter(Project.NAME);
        }

        Project createdProject = catalogService.addProject(project);
        SecurityUtil.addAcl(authorizer, securityContext, Project.NAMESPACE, project.getId(),
                EnumSet.allOf(Permission.class));
        return WSUtils.respondEntity(createdProject, CREATED);
    }

    @PUT
    @Path("/projects/{projectId}")
    @Timed
    public Response editProject(@PathParam("projectId") Long projectId,
                                Project project,
                                @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN,
                NAMESPACE, projectId, WRITE);
        if (StringUtils.isEmpty(project.getName())) {
            throw BadRequestException.missingParameter(Project.NAME);
        }

        Project updatedProject = catalogService.editProject(projectId, project);

        return WSUtils.respondEntity(updatedProject, OK);
    }

    @DELETE
    @Path("/projects/{projectId}")
    @Timed
    public Response removeProject(@PathParam("projectId") Long projectId,
                                   @javax.ws.rs.QueryParam("force") boolean force,
                                   @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                NAMESPACE, projectId, DELETE);

        Project result = catalogService.getProjectInfo(projectId);
        if (result == null) {
            throw EntityNotFoundException.byId(projectId.toString());
        }

        Project removedProject = catalogService.removeProject(projectId);
        if (removedProject != null) {
            SecurityUtil.removeAcl(authorizer, securityContext, NAMESPACE, projectId);
            return WSUtils.respondEntity(removedProject, OK);
        }
        throw EntityNotFoundException.byId(projectId.toString());
    }

    @GET
    @Path("/projects/{projectId}/topologies")
    @Timed
    public Response listTopologies (@PathParam("projectId") Long projectId,
                                    @Context SecurityContext securityContext, @Session HttpSession httpSession) {

        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        return listTopologies(securityContext, userGroups, projectId);
    }


    @GET
    @Path("/topologies")
    @Timed
    public Response listTopologiesByProjectId (@javax.ws.rs.QueryParam("projectId") Long projectId,
                                               @Context SecurityContext securityContext,
                                               @DefaultValue("false") @javax.ws.rs.QueryParam("withcount") Boolean withCount,
                                               @DefaultValue("0")   @javax.ws.rs.QueryParam("offset") Long offset,
                                               @DefaultValue("10")  @javax.ws.rs.QueryParam("limit") Long limit) {
        if (projectId == null || projectId == StreamCatalogService.PLACEHOLDER_ID) {
            return listTopologies(securityContext, StreamCatalogService.PLACEHOLDER_ID, offset, limit, withCount);
        } else {
            return listTopologies(securityContext, projectId, offset, limit, withCount);
        }
    }

    @GET
    @Path("/topologies/search")
    @Timed
    public Response searchTopologiesByName (@javax.ws.rs.QueryParam("name") String topologyName,
                                            @Context SecurityContext securityContext) {
        boolean adminUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_ADMIN);
        Collection<Topology> topologies = catalogService.listTopologies(Collections.singletonList(
                new com.hortonworks.streamline.common.QueryParam(Topology.NAME, topologyName)));

        if (adminUser) {
            LOG.debug("Returning all topologies since user has role: {}", Roles.ROLE_ADMIN);
        } else {
            topologies = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, topologies, READ);
        }

        Response response;
        if (topologies != null) {
            response = WSUtils.respondEntities(topologies, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }
        return response;
    }


    @GET
    @Path("/topologies/{topologyId}/runtimeApplicationId")
    @Timed
    public Response getRuntimeApplicationId (@PathParam("topologyId") Long topologyId,
                                             @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);

        Topology topology = Optional.ofNullable(catalogService.getTopology(topologyId))
                .orElseThrow(() -> EntityNotFoundException.byId(topologyId.toString()));
        Collection<TopologyRuntimeIdMap> topologyRuntimeIdMapList = actionsService.getRuntimeTopologyId(topology);
        if (!topologyRuntimeIdMapList.isEmpty()) {
            TopologyRuntimeIdMap topologyRuntimeIdMap = topologyRuntimeIdMapList.iterator().next();
            return WSUtils.respondEntity(topologyRuntimeIdMap, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}")
    @Timed
    public Response getTopologyById(@PathParam("topologyId") Long topologyId,
                                    @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);

        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}")
    @Timed
    public Response shareTopology(@PathParam("topologyId") Long topologyId,
                                  @Context SecurityContext securityContext,
                                  @javax.ws.rs.QueryParam("sidId") long sidId,
                                  @javax.ws.rs.QueryParam("permission") String permission,
                                  @Session HttpSession httpSession) {
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER, userGroups,
                NAMESPACE, topologyId, READ);
        Topology topology = Optional.ofNullable(catalogService.getTopology(topologyId))
                .orElseThrow(() -> EntityNotFoundException.byId(topologyId.toString()));

        AclEntry aclEntry = SecurityUtil.addAcl(authorizer, securityContext, Topology.NAMESPACE, topology.getId(), sidId, EnumSet.of(Permission.valueOf(permission.toUpperCase())));
        // Also, giving read permission to the Project
        SecurityUtil.addAcl(authorizer, securityContext, Project.NAMESPACE, topology.getProjectId(), sidId, EnumSet.of(Permission.READ));
        return WSUtils.respondEntity(aclEntry, CREATED);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}")
    @Timed
    public Response getTopologyByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                              @PathParam("versionId") Long versionId,
                                              @Context SecurityContext securityContext,
                                              @Session HttpSession httpSession) {
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER, userGroups,
                NAMESPACE, topologyId, READ);

        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/versions")
    @Timed
    public Response listTopologyVersions(@PathParam("topologyId") Long topologyId, @Context SecurityContext securityContext, @Session HttpSession httpSession) {
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER, userGroups,
                NAMESPACE, topologyId, READ);
        Collection<TopologyVersion> versionInfos = catalogService.listTopologyVersionInfos(
                WSUtils.buildTopologyIdAwareQueryParams(topologyId, null));
        Response response;
        if (versionInfos != null) {
            response = WSUtils.respondEntities(versionInfos, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }
        return response;
    }

    @POST
    @Path("/projects/{projectId}/topologies")
    @Timed
    public Response addTopology(@PathParam("projectId") Long projectId, Topology topology,
                                @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        if (StringUtils.isEmpty(topology.getName())) {
            throw BadRequestException.missingParameter(Topology.NAME);
        }

        topology.setProjectId(projectId);
        Topology createdTopology = catalogService.addTopology(topology);
        SecurityUtil.addAcl(authorizer, securityContext, Topology.NAMESPACE, createdTopology.getId(),
                EnumSet.allOf(Permission.class));
        // Add OwnerGroup level ACL
        addAclForTopology(createdTopology.getId(), topology.getOwnergroups());
        return WSUtils.respondEntity(createdTopology, CREATED);
    }

    private void addAclForTopology(Long topologyID, String groups) {
        if (groups != null) {
            String[] data = groups.split(",");
            Set<String> userGroupNames = new HashSet<>(Arrays.asList(data));
            Set<OwnerGroup> userOwnerGroups = securityCatalogService.getGroups(userGroupNames);
            userOwnerGroups.forEach((ownerGroup) -> {
                SecurityUtil.addAcl(authorizer, Topology.NAMESPACE, topologyID,
                        AclEntry.SidType.GROUP, ownerGroup.getId(), EnumSet.allOf(Permission.class));
            });
        }
    }

    @DELETE
    @Path("/topologies/{topologyId}")
    @Timed
    public Response removeTopology(@PathParam("topologyId") Long topologyId,
                                   @javax.ws.rs.QueryParam("onlyCurrent") boolean onlyCurrent,
                                   @javax.ws.rs.QueryParam("force") boolean force,
                                   @Context SecurityContext securityContext,
                                   @Session HttpSession httpSession)  {
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, DELETE);

        Topology result = catalogService.getTopology(topologyId);
        if (result == null) {
            throw EntityNotFoundException.byId(topologyId.toString());
        }

        Collection<TopologyRuntimeIdMap> topologyRuntimeIdMapList = actionsService.getRuntimeTopologyId(result);
        if (!force && !result.getNamespaceId().equals(EnvironmentService.TEST_ENVIRONMENT_ID)
                && !topologyRuntimeIdMapList.isEmpty()) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            actionsService.killTopology(result, asUser);
        }

        catalogService.removeTopologyRuntimeIdMap(topologyId);
        Response response;
        if (onlyCurrent) {
            response = removeCurrentTopologyVersion(topologyId);
        } else {
            response = removeAllTopologyVersions(topologyId);
        }
        SecurityUtil.removeAcl(authorizer, securityContext, NAMESPACE, topologyId);
        return response;
    }

    private Response removeAllTopologyVersions(Long topologyId) {
        Collection<TopologyVersion> versions = catalogService.listTopologyVersionInfos(
                WSUtils.topologyVersionsQueryParam(topologyId));
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        Topology res = null;
        for (TopologyVersion version : versions) {
            Topology removed = catalogService.removeTopology(topologyId, version.getId(), true);
            if (removed != null && removed.getVersionId().equals(currentVersionId)) {
                res = removed;
            }
        }
        // remove topology state information
        catalogService.removeTopologyState(topologyId);
        if (res != null) {
            return WSUtils.respondEntity(res, OK);
        } else {
            throw EntityNotFoundException.byId(topologyId.toString());
        }
    }

    private Response removeCurrentTopologyVersion(Long topologyId) {
        Topology removedTopology = catalogService.removeTopology(topologyId, true);
        if (removedTopology != null) {
            return WSUtils.respondEntity(removedTopology, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @PUT
    @Path("/topologies/{topologyId}")
    @Timed
    public Response addOrUpdateTopology (@PathParam("topologyId") Long topologyId,
                                         Topology topology,
                                         @Context SecurityContext securityContext,
                                         @Session HttpSession httpSession) {
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, WRITE);
        if (StringUtils.isEmpty(topology.getName())) {
            throw BadRequestException.missingParameter(Topology.NAME);
        }
        if (topology.getConfig() == null) {
            throw BadRequestException.missingParameter(Topology.CONFIG);
        }
        if (topology.getNamespaceId() == null) {
            throw BadRequestException.missingParameter(Topology.NAMESPACE_ID);
        }

        Topology existingTopology = catalogService.getTopology(topologyId);
        Topology result = catalogService.addOrUpdateTopology(topologyId, topology);


        if (existingTopology != null) {
            Long prevNamespaceId = existingTopology.getNamespaceId();
            if (!result.getNamespaceId().equals(prevNamespaceId)) {
                LOG.info("Determined namespace change on topology: " + topologyId);
                // environment has changed: it should set 'reconfigure' to all components
                catalogService.setReconfigureOnAllComponentsInTopology(result);
            }
        }
        return WSUtils.respondEntity(result, OK);
    }

    /**
     * {
     *     "name": "v2",
     *     "description": "saved before prod deployment"
     * }
     */
    @POST
    @Path("/topologies/{topologyId}/versions/save")
    @Timed
    public Response saveTopologyVersion(@PathParam("topologyId") Long topologyId, TopologyVersion versionInfo,
                                        @Context SecurityContext securityContext, @Session HttpSession httpSession) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER, userGroups,
                NAMESPACE, topologyId, READ);
        Optional<TopologyVersion> currentVersion = Optional.empty();
        try {
            currentVersion = catalogService.getCurrentTopologyVersionInfo(topologyId);
            if (!currentVersion.isPresent()) {
                throw new IllegalArgumentException("Current version is not available for topology id: " + topologyId);
            }
            if (versionInfo == null) {
                versionInfo = new TopologyVersion();
            }
            // update the current version with the new version info.
            versionInfo.setTopologyId(topologyId);
            Optional<TopologyVersion> latest = catalogService.getLatestVersionInfo(topologyId);
            int suffix;
            if (latest.isPresent()) {
                suffix = latest.get().getVersionNumber() + 1;
            } else {
                suffix = 1;
            }
            versionInfo.setName(VERSION_PREFIX + suffix);
            if (versionInfo.getDescription() == null) {
                versionInfo.setDescription("");
            }
            if (versionInfo.getDagThumbnail() == null) {
                versionInfo.setDagThumbnail("");
            }
            TopologyVersion savedVersion = catalogService.addOrUpdateTopologyVersionInfo(
                    currentVersion.get().getId(), versionInfo);
            catalogService.cloneTopologyVersion(topologyId, savedVersion.getId());
            return WSUtils.respondEntity(savedVersion, CREATED);
        } catch (Exception ex) {
            // restore the current version
            if (currentVersion.isPresent()) {
                catalogService.addOrUpdateTopologyVersionInfo(currentVersion.get().getId(), currentVersion.get());
            }
            throw ex;
        }
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/activate")
    @Timed
    public Response activateTopologyVersion(@PathParam("topologyId") Long topologyId,
                                            @PathParam("versionId") Long versionId,
                                            @Context SecurityContext securityContext,
                                            @Session HttpSession httpSession) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER, userGroups,
                NAMESPACE, topologyId, READ);
        Optional<TopologyVersion> currentVersionInfo = catalogService.getCurrentTopologyVersionInfo(topologyId);
        if (currentVersionInfo.isPresent() && currentVersionInfo.get().getId().equals(versionId)) {
            throw new IllegalArgumentException("Version id " + versionId + " is already the current version");
        }
        TopologyVersion savedVersion = catalogService.getTopologyVersionInfo(versionId);
        if (savedVersion != null) {
            TopologyVersion newVersionInfo = new TopologyVersion();
            // update the current version with the new version info.
            newVersionInfo.setTopologyId(topologyId);
            Optional<TopologyVersion> latest = catalogService.getLatestVersionInfo(topologyId);
            int suffix;
            if (latest.isPresent()) {
                suffix = latest.get().getVersionNumber() + 1;
            } else {
                suffix = 1;
            }
            newVersionInfo.setName(VERSION_PREFIX + suffix);
            if (currentVersionInfo.get().getDescription() != null) {
                newVersionInfo.setDescription(currentVersionInfo.get().getDescription());
            }
            if (currentVersionInfo.get().getDagThumbnail() != null) {
                newVersionInfo.setDagThumbnail(currentVersionInfo.get().getDagThumbnail());
            }
            catalogService.addOrUpdateTopologyVersionInfo(
                    currentVersionInfo.get().getId(), newVersionInfo);
            // Make older version as new 'CURRENT'
            catalogService.addOrUpdateTopologyVersionInfo(
                    savedVersion.getId(), currentVersionInfo.get());

            return WSUtils.respondEntity(savedVersion, CREATED);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/deploymentstate")
    @Timed
    public Response topologyDeploymentState(@PathParam("topologyId") Long topologyId) throws Exception {
        return Optional.ofNullable(catalogService.getTopology(topologyId))
                .flatMap(t -> catalogService.getTopologyState(t.getId()))
                .map(s -> WSUtils.respondEntity(s, OK))
                .orElseThrow(() -> EntityNotFoundException.byId(topologyId.toString()));
    }

    @GET
    @Path("/topologies/{topologyId}/actions/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public Response exportTopology(@PathParam("topologyId") Long topologyId, @Context SecurityContext securityContext, @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            String exportedTopology = catalogService.exportTopology(topology);
            if (!StringUtils.isEmpty(exportedTopology)) {
                InputStream is = new ByteArrayInputStream(exportedTopology.getBytes(StandardCharsets.UTF_8));
                return Response.status(OK)
                        .entity(is)
                        .header("Content-Disposition", "attachment; filename=\"" + topology.getName() + ".json\"")
                        .build();
            }
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/clone")
    @Timed
    public Response cloneTopology(@PathParam("topologyId") Long topologyId,
                                  @QueryParam("namespaceId") Long namespaceId,
                                  @QueryParam("projectId") Long projectId,
                                  @Context SecurityContext securityContext,
                                  @Session HttpSession httpSession) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology originalTopology = catalogService.getTopology(topologyId);
        if (originalTopology != null) {
            Topology clonedTopology = catalogService.cloneTopology(namespaceId, projectId, originalTopology);
            SecurityUtil.addAcl(authorizer, securityContext, Topology.NAMESPACE, clonedTopology.getId(),
                    EnumSet.allOf(Permission.class));
            // Add OwnerGroup level ACL
            addAclForTopology(clonedTopology.getId(), originalTopology.getOwnergroups());
            return WSUtils.respondEntity(clonedTopology, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    /**
     * curl -X POST 'http://localhost:8080/api/v1/catalog/topologies/actions/import' -F file=@/tmp/topology.json -F namespaceId=1
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/topologies/actions/import")
    @Timed
    public Response importTopology(@FormDataParam("file") final InputStream inputStream,
                                   @FormDataParam("namespaceId") final Long namespaceId,
                                   @FormDataParam("topologyName") final String topologyName,
                                   @FormDataParam("projectId") final Long projectId,
                                   @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        if (namespaceId == null) {
            throw new IllegalArgumentException("Missing namespaceId");
        }
        TopologyData topologyData = new ObjectMapper().readValue(inputStream, TopologyData.class);
        if (topologyName != null && !topologyName.isEmpty()) {
            topologyData.setTopologyName(topologyName);
            updateOwner(topologyData, securityContext);
        }
        Topology importedTopology = catalogService.importTopology(namespaceId, projectId, topologyData);
        SecurityUtil.addAcl(authorizer, securityContext, Topology.NAMESPACE, importedTopology.getId(),
                EnumSet.allOf(Permission.class));
        // Add OwnerGroup level ACL
        addAclForTopology(importedTopology.getId(), importedTopology.getOwnergroups());
        return WSUtils.respondEntity(importedTopology, OK);
    }

    private void updateOwner(TopologyData topologyData, SecurityContext securityContext) {
        topologyData.getConfig().put(PIPER_TOPOLOGY_CONFIG_OWNER, securityCatalogService.getCurrentUserName(securityContext));
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/topologies/actions/importPiperContract")
    @Timed
    public Response importPiperContract(@FormDataParam("file") final InputStream inputStream,
                                        @javax.ws.rs.QueryParam("testMode") boolean testMode,
                                        @Context SecurityContext securityContext) throws Exception {

        PiperContractImporter pci = new PiperContractImporter();
        return pci.importTopology(inputStream, testMode, securityContext);
    }

    @DELETE
    @Path("/topologies/importedPiperContract/{topologyId}")
    @Timed
    public Response removePiperContractTopology(@PathParam("topologyId") Long topologyId,
                                                @javax.ws.rs.QueryParam("onlyCurrent") boolean onlyCurrent,
                                                @javax.ws.rs.QueryParam("force") boolean force,
                                                @Context SecurityContext securityContext,
                                                @Session HttpSession httpSession) {

        PiperContractImporter pci = new PiperContractImporter();
        return pci.removeTopology(topologyId, onlyCurrent, force, securityContext, httpSession);
    }

    @GET
    @Path("/topologies/{topologyId}/reconfigure")
    @Timed
    public Response getComponentsToReconfigure(@PathParam("topologyId") Long topologyId,
                                               @Context SecurityContext securityContext,
                                               @Session HttpSession httpSession) throws Exception {
        Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER, userGroups,
                NAMESPACE, topologyId, READ);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            return WSUtils.respondEntity(catalogService.getComponentsToReconfigure(topology), OK);
        }
        throw EntityNotFoundException.byId(topologyId.toString());
    }

    private Response listTopologies(SecurityContext securityContext, Set<String> userGroups, Long projectId) {
        boolean adminUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_ADMIN);
        Collection<Topology> topologies;
        if (projectId == null) {
            topologies = catalogService.listTopologies();
        } else {
            topologies = catalogService.listTopologiesWithVersionName(projectId);
        }
        if (adminUser) {
            LOG.debug("Returning all topologies since user has role: {}", Roles.ROLE_ADMIN);
        } else {
            topologies = SecurityUtil.filter(authorizer, securityContext, userGroups, NAMESPACE, topologies, READ);
        }

        Response response;
        if (topologies != null) {
            response = WSUtils.respondEntities(topologies, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }
        return response;
    }

    private Response listTopologies (SecurityContext securityContext, Long projectId, long offset, long limit, boolean withCount) {
        boolean topologyUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER);
        Collection<Topology> topologies;
        Collection<Topology>  topologiesWithCount = Collections.EMPTY_LIST;
        if (projectId == null) {
            topologies = catalogService.listTopologies(offset, limit);
            if (withCount) {
                topologiesWithCount = catalogService.listTopologies(OFFSET, LIMIT);
            }
        } else {
            topologies = catalogService.listTopologies(projectId, offset, limit);
            if (withCount) {
                topologiesWithCount = catalogService.listTopologies(projectId, OFFSET, LIMIT);
            }
        }

        if (topologyUser) {
            LOG.debug("Returning all topologies since user has role: {}", Roles.ROLE_TOPOLOGY_USER);
        } else {
            topologies = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, topologies, READ);
            if (withCount) {
                topologiesWithCount = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, topologiesWithCount, READ);
            }
        }

        if (withCount) {
            TopologiesWithCountDto topologiesWithCountDto = new TopologiesWithCountDto();
            topologiesWithCountDto.setTotalRecords(topologiesWithCount.size());
            topologiesWithCountDto.setTopologies(topologies);
            return WSUtils.respondEntity(topologiesWithCountDto, OK);
        }

        Response response;
        if (topologies != null) {
            response = WSUtils.respondEntities(topologies, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }
        return response;
    }

    /*
        Temporary helper class for Piper Contract Migration.  This should be removed post migration (~August 2019)
     */
    public class PiperContractImporter {

        // Import Piper Contract, set runtime id map so that Topology is linked with already running Managed Pipeline.  Add
        // owner ldap groups as necessary and set ACLs.
        public Response importTopology(InputStream inputStream, Boolean testMode, SecurityContext securityContext) throws Exception {

            LOG.debug("** PiperContractImporter.importTopology ********************************************");

            SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);

            TopologyData topologyData = new ObjectMapper().readValue(inputStream, TopologyData.class);

            Config pipelineConfig = topologyData.getConfig();

            if (pipelineConfig == null) {
                throw new IllegalArgumentException("Missing config");
            }

            String topologyName = topologyData.getTopologyName();

            if (topologyName == null) {
                throw new IllegalArgumentException("Missing topologyName");
            }

            String namespaceIdStr = pipelineConfig.get("topology.namespaceIds");

            if (StringUtils.isEmpty(namespaceIdStr)) {
                throw new IllegalArgumentException("Missing config.topology.namespaceIds");
            }

            List<Long> namespaceIds = namespaceIdsToList(namespaceIdStr);
            Long namespaceId = namespaceIds.get(0);

            if (namespaceId == null) {
                throw new IllegalArgumentException("Need at least one namepspace id");
            }

            // Type in config is String
            Long projectId = pipelineConfig.getLong("topology.projectId");

            Topology importedTopology = catalogService.importTopology(namespaceId, projectId, topologyData);

            // Add permisions for owner to acl table
            SecurityUtil.addAcl(authorizer, securityContext, Topology.NAMESPACE, importedTopology.getId(),
                    EnumSet.allOf(Permission.class));


            String groups = pipelineConfig.get("topology.ownerLDAPGroups");
            if (StringUtils.isNotEmpty(groups)) {
                Set<String> groupSet = new HashSet<>(Arrays.asList(groups.split(",")));
                securityCatalogService.addMissingGroupsFromUser(groupSet);
                addAclForTopology(importedTopology.getId(), groups);
            }

            // Add OwnerGroup level ACL
            // FIXME from original import method, it doesn't work leaving for now
            addAclForTopology(importedTopology.getId(), importedTopology.getOwnergroups());

            String runtimeId = pipelineConfig.get("topology.runtimeId");
            if (StringUtils.isNotEmpty(runtimeId) && !testMode) {

                List<TopologyActions.DeployedRuntimeId> deployedRuntimeIds = new ArrayList<>();

                for (Long deployedNamespaceId: namespaceIds) {
                    deployedRuntimeIds.add(new TopologyActions.DeployedRuntimeId(deployedNamespaceId, runtimeId));
                }

                // Link the topology to an the already running contract.
                // Note:  deleting the topology through UI after linking will delete a live Piper Contract
                actionsService.updateRuntimeApplicationId(importedTopology, deployedRuntimeIds);

                // Set the import state to TOPOLOGY_STATE_DEPLOYED, metrics should be live at this point
                setImportState(importedTopology);

                // deploy topology (this is effectively a re-deploy) to confirm working
                actionsService.deployTopology(importedTopology, null);

                // generate a new version to be consistent with UI which always creates a draft after deploy
                save(importedTopology);
            }

            return WSUtils.respondEntity(importedTopology, OK);
        }


        // Remove an imported Piper Contract.  We don't want to delete the MP on Piper, so this method removes
        // the topology and the runtime app id.
        public Response removeTopology(Long topologyId, boolean onlyCurrent, boolean force, SecurityContext securityContext,
                                       HttpSession httpSession) {

            Set<String> userGroups = SecurityUtil.getAllUserGroups(httpSession);
            SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN, userGroups,
                    NAMESPACE, topologyId, DELETE);

            Topology result = catalogService.getTopology(topologyId);
            if (result == null) {
                throw EntityNotFoundException.byId(topologyId.toString());
            }

            // We don't want to delete the MP on Piper, so don't call killTopology

            /*
            Collection<TopologyRuntimeIdMap> topologyRuntimeIdMapList = actionsService.getRuntimeTopologyId(result);
            if (!force && !result.getNamespaceId().equals(EnvironmentService.TEST_ENVIRONMENT_ID)
                    && !topologyRuntimeIdMapList.isEmpty()) {
                String asUser = WSUtils.getUserFromSecurityContext(securityContext);
                actionsService.killTopology(result, asUser);
            }
            */

            catalogService.removeTopologyRuntimeIdMap(topologyId);
            Response response;
            if (onlyCurrent) {
                response = removeCurrentTopologyVersion(topologyId);
            } else {
                response = removeAllTopologyVersions(topologyId);
            }
            SecurityUtil.removeAcl(authorizer, securityContext, NAMESPACE, topologyId);
            return response;

        }

        private List<Long> namespaceIdsToList(String namespaceIdStr) {
            LOG.debug("** importTopology ********************************************");
            List<Long> regions = null;
            if (!StringUtils.isEmpty(namespaceIdStr)) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    regions = mapper.readValue(namespaceIdStr, new TypeReference<List<Long>>() {
                    });
                } catch(IOException e) {
                    throw new IllegalArgumentException("Failed to parse topology deployment settings for");
                }
            }
            return regions;

        }

        private void setImportState(Topology topology) {
            LOG.debug("** setImportState ********************************************");
            com.hortonworks.streamline.streams.catalog.topology.state.TopologyState catalogState =
                    new com.hortonworks.streamline.streams.catalog.topology.state.TopologyState();

            catalogState.setName("TOPOLOGY_STATE_DEPLOYED");
            catalogState.setTopologyId(topology.getId());
            catalogState.setDescription("Migrated");
            LOG.debug("Topology id: {}, state: {}", topology.getId(), catalogState);
            actionsService.getCatalogService().addOrUpdateTopologyState(topology.getId(), catalogState);
        }

        private void save(Topology topology) {
            LOG.debug("** save ********************************************");
            Optional<TopologyVersion> currentVersion = Optional.empty();
            Long topologyId = topology.getId();
            // Copied from save endpoint, delete after contract migration
            try {
                currentVersion = catalogService.getCurrentTopologyVersionInfo(topologyId);
                if (!currentVersion.isPresent()) {
                    throw new IllegalArgumentException("Current version is not available for topology id: " + topologyId);
                }
                TopologyVersion versionInfo = new TopologyVersion();
                // update the current version with the new version info.
                versionInfo.setTopologyId(topologyId);
                Optional<TopologyVersion> latest = catalogService.getLatestVersionInfo(topologyId);
                int suffix;
                if (latest.isPresent()) {
                    suffix = latest.get().getVersionNumber() + 1;
                } else {
                    suffix = 1;
                }
                versionInfo.setName(VERSION_PREFIX + suffix);
                if (versionInfo.getDescription() == null) {
                    versionInfo.setDescription("");
                }
                if (versionInfo.getDagThumbnail() == null) {
                    versionInfo.setDagThumbnail("");
                }
                TopologyVersion savedVersion = catalogService.addOrUpdateTopologyVersionInfo(
                        currentVersion.get().getId(), versionInfo);
                catalogService.cloneTopologyVersion(topologyId, savedVersion.getId());
            } catch (Exception ex) {
                // restore the current version
                if (currentVersion.isPresent()) {
                    catalogService.addOrUpdateTopologyVersionInfo(currentVersion.get().getId(), currentVersion.get());
                }
                throw ex;
            }
        }
    }
}