package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.registry.table.CreateTableException;
import com.hortonworks.streamline.streams.registry.table.DeployTableException;
import com.hortonworks.streamline.streams.registry.table.TableNotFoundException;
import com.hortonworks.streamline.streams.registry.table.DataSchemaServiceClient;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static javax.ws.rs.core.Response.Status.*;

@Path("/v1/tables")
@Produces(MediaType.APPLICATION_JSON)
public class DataSchemaResource {

    private final DataSchemaServiceClient dataSchemaServiceClient;

    public DataSchemaResource(DataSchemaServiceClient dataSchemaServiceClient) {
        this.dataSchemaServiceClient = dataSchemaServiceClient;
    }

    @GET
    @Path("/{tableName}")
    @Timed
    public Response getTableSchema(@PathParam("tableName") String tableName, @Context SecurityContext securityContext) {
        try {
            String tableSchema = dataSchemaServiceClient.getTableSchema(tableName);
            return WSUtils.respondEntity(tableSchema, OK);
        } catch (TableNotFoundException e) {
            return WSUtils.respondEntity("Cannot find table with name " + tableName, NOT_FOUND);
        }
    }

    @POST
    @Timed
    public Response createTable(Object createTableRequest, @Context SecurityContext securityContext) {
        try {
            dataSchemaServiceClient.createTable(createTableRequest);
            return WSUtils.respondEntity("Table created", CREATED);
        } catch (CreateTableException e) {
            return WSUtils.respondEntity("Cannot create table with request " + createTableRequest, BAD_REQUEST);
        }
    }

    @POST
    @Path("/{tableName}")
    @Timed
    public Response deployTable(@PathParam("tableName") String tableName, Object deployTableRequest, @Context SecurityContext securityContext) {
        try {
            dataSchemaServiceClient.deployTable(deployTableRequest, tableName);
            return WSUtils.respondEntity("Table deployed", OK);
        } catch (DeployTableException e) {
            return WSUtils.respondEntity("Cannot deploy table with request " + deployTableRequest, BAD_REQUEST);
        }
    }
}
