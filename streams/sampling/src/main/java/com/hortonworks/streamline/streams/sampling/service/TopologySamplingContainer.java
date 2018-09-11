package com.hortonworks.streamline.streams.sampling.service;

import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.container.NamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.sampling.service.mapping.MappedTopologySamplingImpl;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;

public class TopologySamplingContainer extends NamespaceAwareContainer<TopologySampling> {
    private final Subject subject;

    public TopologySamplingContainer(EnvironmentService environmentService, Subject subject) {
        super(environmentService);
        this.subject = subject;
    }

    @Override
    protected TopologySampling initializeInstance(Namespace namespace) {
        try {
            String engine = namespace.getEngine();

            MappedTopologySamplingImpl samplingImpl;
            try {
                samplingImpl = MappedTopologySamplingImpl.valueOf(engine);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Unsupported engine: " + engine, e);
            }

            Class<TopologySampling> clazz = (Class<TopologySampling>) Class.forName(samplingImpl.getClassName());
            TopologySampling samplingInstance = clazz.newInstance();
            samplingInstance.init(buildConfig(namespace, engine));
            return samplingInstance;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Map<String, Object> buildConfig(Namespace namespace, String engine) {
        Map<String, Object> conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY, buildStormRestApiRootUrl(namespace, engine));
        conf.put(TopologyLayoutConstants.SUBJECT_OBJECT, subject);
        return conf;
    }

}
