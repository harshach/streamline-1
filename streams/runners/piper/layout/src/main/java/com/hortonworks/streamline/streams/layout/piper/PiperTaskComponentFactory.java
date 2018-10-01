package com.hortonworks.streamline.streams.layout.piper;

import com.hortonworks.streamline.streams.layout.component.StreamlineComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for loading Piper Task components
 */
public class PiperTaskComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PiperTaskComponentFactory.class);

    /**
     * Return PiperTaskComponent based on transformationClass
     * @param streamlineComponent
     * @return
     */
    public PiperTaskComponent getPiperTaskComponent(StreamlineComponent streamlineComponent) {
        String className = streamlineComponent.getTransformationClass();
        try {
            Class<PiperTaskComponent> clazz = (Class<PiperTaskComponent>)Class.forName(className);
            return clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Error while creating Piper task component for class " + className, e);
            throw new RuntimeException(e);
        }
    }
}
