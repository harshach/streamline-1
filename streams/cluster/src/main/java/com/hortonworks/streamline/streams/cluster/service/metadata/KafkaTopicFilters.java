package com.hortonworks.streamline.streams.cluster.service.metadata;

public class KafkaTopicFilters {

    public static boolean noop(String topicName) {
        return true;
    }

    public static boolean isHeatpipeTopic(String topicName) {
        return (topicName.startsWith("hp.") || topicName.startsWith("hp-") || topicName.startsWith("hp_"));
    }
}
