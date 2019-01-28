package com.hortonworks.streamline.streams.exception;

import com.hortonworks.streamline.common.exception.service.exception.WebServiceException;

import javax.ws.rs.core.Response;

public class FailedToKillDeployedTopologyException extends WebServiceException {
    private static final String MESSAGE = "Failed to kill deployed topology with runTimeId [%s] .";
    private static final String MESSAGE_HAVING_ADDITIONAL_MESSAGE = "Failed to kill deployed topology [%s]. An exception with message [%s] was thrown.";

    public FailedToKillDeployedTopologyException(String runTimeId) {
        super(Response.Status.INTERNAL_SERVER_ERROR, String.format(MESSAGE, runTimeId));
    }

    public FailedToKillDeployedTopologyException(Throwable e) {
        super(Response.Status.INTERNAL_SERVER_ERROR, MESSAGE, e);
    }

    public FailedToKillDeployedTopologyException(String runTimeId, String exceptionMessage) {
        super(Response.Status.INTERNAL_SERVER_ERROR, String.format(MESSAGE_HAVING_ADDITIONAL_MESSAGE, runTimeId,exceptionMessage));
    }

    public FailedToKillDeployedTopologyException(String runTimeId, String exceptionMessage, Throwable cause) {
        super(Response.Status.INTERNAL_SERVER_ERROR, String.format(MESSAGE_HAVING_ADDITIONAL_MESSAGE, runTimeId, exceptionMessage), cause);
    }
}

