package com.hortonworks.streamline.streams.cluster.resource;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.util.Map.Entry;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/health")
public class HealthCheckResource {
    private HealthCheckRegistry registry;

    public HealthCheckResource(HealthCheckRegistry registry) {
        this.registry = registry;
    }

    @GET
    public Set<Entry<String, Result>> getStatus() {
        return registry.runHealthChecks().entrySet();
    }
}
