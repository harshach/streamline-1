package com.hortonworks.streamline.streams.cluster.model;

import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;

import java.util.Map;

public class NamespaceWithServiceConfigurationMap {
    private Namespace namespace;
    private Service service;
    private Map<String, ServiceConfiguration> serviceConfigurationMap;

    public NamespaceWithServiceConfigurationMap(Namespace namespace) {
        this.namespace = namespace;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Map<String, ServiceConfiguration> getServiceConfigurationMap() {
        return serviceConfigurationMap;
    }

    public void setServiceConfigurationMap(Map<String, ServiceConfiguration> serviceConfigurationMap) {
        this.serviceConfigurationMap = serviceConfigurationMap;
    }
}
