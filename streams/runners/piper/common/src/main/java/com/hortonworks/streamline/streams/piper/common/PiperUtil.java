package com.hortonworks.streamline.streams.piper.common;

import java.util.Map;

import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RUNTIME_STATUS_UNKNOWN;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_ACTIVE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_PAUSED;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RUNTIME_STATUS_ENABLED;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RUNTIME_STATUS_PAUSED;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.PIPER_RUNTIME_STATUS_INACTIVE;
import static com.hortonworks.streamline.streams.piper.common.PiperConstants.STATE_KEY_EXECUTION_TS;

public class PiperUtil {
    public static String getRuntimeStatus(Map<String, Object> stateResponse) {
        String runtimeStatus = PIPER_RUNTIME_STATUS_UNKNOWN;

        if (stateResponse != null) {
            Boolean active = (Boolean) stateResponse.get(STATE_KEY_ACTIVE);
            Boolean paused = (Boolean) stateResponse.get(STATE_KEY_PAUSED);

            if (active != null &&  paused != null) {
                if (active && !paused) {
                    runtimeStatus = PIPER_RUNTIME_STATUS_ENABLED;
                } else if (active) {
                    runtimeStatus = PIPER_RUNTIME_STATUS_PAUSED;
                } else {
                    runtimeStatus = PIPER_RUNTIME_STATUS_INACTIVE;
                }
            }
        }
        return runtimeStatus;
    }

    // Fix the state response so that date is either null or in the expected format"
    // Package private, no access modifier
    // FIXME - Remove once fixed https://code.uberinternal.com/T2207115
    static Map<String, Object> fixExecutionDate(Map<String, Object> stateResponse) {
        if (stateResponse != null) {
            String corrected = null;
            String latestExecutionDate = (String) stateResponse.get(STATE_KEY_EXECUTION_TS);

            if (latestExecutionDate != null && !"None".equals(latestExecutionDate)) {
                String[] spliced = latestExecutionDate.split(" ");
                if (spliced.length == 2) {
                    corrected = spliced[0] + "T" + spliced[1];
                }
            }
            stateResponse.put("execution_date", corrected);
        }
        return stateResponse;
    }

    public static String buildPiperRestApiRootUrl(String host, String port) {
        return "http://" + host + ":" + port;
    }
}
