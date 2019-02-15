package com.hortonworks.streamline.streams.layout.piper;

import com.hortonworks.streamline.streams.piper.common.pipeline.Pipeline;
import com.hortonworks.streamline.streams.piper.common.pipeline.Task;

import java.util.Map;

/**
 * Interface for PiperTaskComponent
 */
public interface PiperTaskComponent {

    /*
    Method to initialize the implementation with a configuration
     */
    void withConfig (Map<String, Object> config, Pipeline pipeline);

    /**
     * Generate Managed Pipeline Task from component
     */
    Task generateTask();
}
