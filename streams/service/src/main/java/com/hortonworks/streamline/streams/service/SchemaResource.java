/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;
import com.hortonworks.registries.schemaregistry.SchemaIdVersion;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.SchemaVersion;
import com.hortonworks.registries.schemaregistry.errors.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.errors.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.registry.SchemaRegistryClientAdapter;
import com.hortonworks.streamline.streams.registry.StreamlineSchemaNotFoundException;
import com.hortonworks.streamline.streams.registry.StreamlineSchemaRegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static javax.ws.rs.core.Response.Status.OK;

/**
 *
 */
@Path("/v1/schemas")
public class SchemaResource {
    private static final Logger LOG = LoggerFactory.getLogger(SchemaResource.class);

    private final StreamlineSchemaRegistryClient streamlineSchemaRegistryClient;

    public SchemaResource(StreamlineSchemaRegistryClient streamlineSchemaRegistryClient) {
        this.streamlineSchemaRegistryClient = streamlineSchemaRegistryClient;
    }

    @POST
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response postStreamsSchema(StreamsSchemaInfo streamsSchemaInfo,
                                      @Context SecurityContext securityContext) throws IOException {
        Preconditions.checkNotNull(streamsSchemaInfo, "streamsSchemaInfo can not be null");

        if (!(streamlineSchemaRegistryClient instanceof SchemaRegistryClientAdapter)) {
            throw new UnsupportedOperationException("Post to schemas endpoint is supported with only Hortonworks schema-registry.");
        }

        SchemaRegistryClientAdapter schemaRegistryClientAdapter = (SchemaRegistryClientAdapter) streamlineSchemaRegistryClient;
        SchemaMetadata schemaMetadata = streamsSchemaInfo.getSchemaMetadata();
        String schemaName = schemaMetadata.getName();
        Long schemaMetadataId = schemaRegistryClientAdapter.registerSchemaMetadata(schemaMetadata);
        LOG.info("Registered schemaMetadataId [{}] for schema with name:[{}]", schemaMetadataId, schemaName);

        String streamsSchemaText = streamsSchemaInfo.getSchemaVersion().getSchemaText();
        // convert streams schema to avro schema.
        String avroSchemaText = AvroStreamlineSchemaConverter.convertStreamlineSchemaToAvroSchema(streamsSchemaText);
        SchemaVersion avroSchemaVersion = new SchemaVersion(avroSchemaText, streamsSchemaInfo.getSchemaVersion().getDescription());
        try {
            SchemaIdVersion schemaIdVersion = schemaRegistryClientAdapter.addSchemaVersion(schemaName, avroSchemaVersion);
            return WSUtils.respondEntity(schemaIdVersion, OK);
        } catch (InvalidSchemaException e) {
            String errMsg = String.format("Invalid schema received for schema with name [%s] : [%s]", schemaName, streamsSchemaText);
            LOG.error(errMsg, e);
            throw BadRequestException.message(errMsg, e);
        } catch (SchemaNotFoundException e) {
            String errMsg = String.format("Schema not found for topic: [%s]", schemaName);
            LOG.error(errMsg, e);
            throw EntityNotFoundException.byId(schemaName);
        } catch (IncompatibleSchemaException e) {
            String errMsg = String.format("Incompatible schema received for schema with name [%s] : [%s]", schemaName, streamsSchemaText);
            LOG.error(errMsg, e);
            throw BadRequestException.message(errMsg, e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{schemaName}/schema")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentSchema(@PathParam("schemaName") String schemaName,
                                     @Context SecurityContext securityContext) {
        return doGetCurrentSchemaForBranch(schemaName, null);
    }

    @GET
    @Path("/{schemaName}/{branchName}/schema")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentSchemaForBranch(@PathParam("schemaName") String schemaName,
                                              @PathParam("branchName") String branchName,
                                              @Context SecurityContext securityContext) {
        return doGetCurrentSchemaForBranch(schemaName, branchName);
    }

    private Response doGetCurrentSchemaForBranch(String schemaName, String branchName) {
        try {
            LOG.info("Get schema for: {}", schemaName);
            List<Integer> schemaVersions = streamlineSchemaRegistryClient.getSchemaVersions(schemaName, branchName);
            LOG.debug("Received schema versions [{}] for schema: {}", schemaVersions, schemaName);
            Integer highestVersion = schemaVersions.get(schemaVersions.size() - 1);
            String schema = streamlineSchemaRegistryClient.getSchema(schemaName, highestVersion);
            LOG.debug("Received schema version: [{}]", schema);
            schema = AvroStreamlineSchemaConverter.convertAvroSchemaToStreamlineSchema(schema);
            LOG.debug("Converted schema: [{}]", schema);
            return WSUtils.respondEntity(schema, OK);
        } catch (StreamlineSchemaNotFoundException e) {
            LOG.error("Schema not found: [{}]", schemaName, e);
            throw EntityNotFoundException.byName(schemaName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{schemaName}/versions")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSchemaVersions(@PathParam("schemaName") String schemaName,
                                         @Context SecurityContext securityContext) {
        return doGetAllSchemaVersionForBranch(schemaName, null);
    }

    private Response doGetAllSchemaVersionForBranch(String schemaName, String branchName) {
        try {
            LOG.info("Get all versions for schema : {}", schemaName);
            List<Integer> schemaVersions = streamlineSchemaRegistryClient.getSchemaVersions(schemaName, branchName);
            LOG.debug("Received schema versions [{}] for schema: {}", schemaVersions, schemaName);
            return WSUtils.respondEntities(schemaVersions, OK);
        } catch (StreamlineSchemaNotFoundException e) {
            LOG.error("Schema not found: [{}]", schemaName, e);
            throw EntityNotFoundException.byName(schemaName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{schemaName}/{branchName}/versions")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSchemaVersionsForBranch(@PathParam("schemaName") String schemaName,
                                                  @PathParam("branchName") String branchName,
                                                  @Context SecurityContext securityContext) {
        return doGetAllSchemaVersionForBranch(schemaName, branchName);
    }

    @GET
    @Path("/{schemaName}/versions/{version}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchemaForVersion(@PathParam("schemaName") String schemaName, @PathParam("version") String version,
                                        @Context SecurityContext securityContext) {
        try {
            LOG.info("Get schema:version [{}:{}]", schemaName, version);
            String schema = streamlineSchemaRegistryClient.getSchema(schemaName, Integer.parseInt(version));
            LOG.debug("Received schema version: [{}]", schema);
            schema = AvroStreamlineSchemaConverter.convertAvroSchemaToStreamlineSchema(schema);
            LOG.debug("Converted schema: [{}]", schema);
            return WSUtils.respondEntity(schema, OK);
        } catch (StreamlineSchemaNotFoundException e) {
            LOG.error("Schema not found: [{}]", schemaName, e);
            throw EntityNotFoundException.byName(schemaName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{schemaName}/branches")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchemaBranches(@PathParam("schemaName") String schemaName,
                                      @Context SecurityContext securityContext) {
        try {
            List<String> schemaBranchNames = streamlineSchemaRegistryClient.getSchemaBranchNames(schemaName);
            LOG.info("Schema branches for schema [{}]: {}", schemaName, schemaBranchNames);
            return WSUtils.respondEntities(schemaBranchNames, OK);
        } catch (StreamlineSchemaNotFoundException e) {
            LOG.error("Schema not found with name: [{}]", schemaName, e);
            throw EntityNotFoundException.byName(schemaName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
