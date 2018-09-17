package com.hortonworks.streamline.streams.layout.component.impl;


import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.StreamlineTask;



public class GenericTask extends  StreamlineTask {
    public GenericTask(Stream outputStream) {
        addOutputStream(outputStream);
    }

}
