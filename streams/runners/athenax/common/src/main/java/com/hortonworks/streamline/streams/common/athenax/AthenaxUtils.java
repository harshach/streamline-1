package com.hortonworks.streamline.streams.common.athenax;

import com.hortonworks.streamline.streams.common.athenax.entity.JobStatusRequest;
import com.hortonworks.streamline.streams.common.athenax.entity.StopJobRequest;

public class AthenaxUtils {

    public static JobStatusRequest extractJobStatusRequest(String applicationId, String dataCenter, String cluster){
        JobStatusRequest request = new JobStatusRequest();
        request.setDataCenter(dataCenter);
        request.setCluster(cluster);
        request.setYarnApplicationId(applicationId);
        return request;
    }

    public static StopJobRequest extractStopJobRequest(String applicationId, String dataCenter, String cluster) {
        StopJobRequest request = new StopJobRequest();
        request.setDataCenter(dataCenter);
        request.setCluster(cluster);
        request.setAppId(applicationId);
        return request;
    }
}
