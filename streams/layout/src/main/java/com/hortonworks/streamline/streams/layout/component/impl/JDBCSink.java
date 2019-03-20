package com.hortonworks.streamline.streams.layout.component.impl;

import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;

public class JDBCSink extends StreamlineSink {
	@Override
	public void accept(TopologyDagVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
