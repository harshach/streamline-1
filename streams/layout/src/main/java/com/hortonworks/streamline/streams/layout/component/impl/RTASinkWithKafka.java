package com.hortonworks.streamline.streams.layout.component.impl;

import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;

public class RTASinkWithKafka extends RTASink {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
