package com.hortonworks.streamline.common.credentials;

import com.uber.engsec.upkiclient.UPKIClient;
import com.uber.engsec.upkiclient.UPKIClientFactory;
import com.uber.engsec.upkiclient.UPKITokenService;
import com.uber.engsec.upkiclient.config.SpiffeConfig;
import com.uber.engsec.upkiclient.utoken.UToken;
import com.uber.engsec.upkiclient.utoken.UTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPKITokenManager {

    private static final Logger LOG = LoggerFactory.getLogger(UPKITokenManager.class);

    private UPKITokenService upkiTokenService;
    private final String upkiSpiffeEndPoint = "/var/cache/udocker_mnt/worf.sock";
    private static UPKITokenManager upkiTokenManagerInstance;

    private UPKITokenManager() {
        if (upkiTokenManagerInstance != null) {
            throw new RuntimeException("use acquireUPKITokenManager() to get instance of a UPKITokenManager");
        }

        try {
            SpiffeConfig.SpiffeConfigBuilder builder = new SpiffeConfig.SpiffeConfigBuilder().
                    withSpiffeEndpointSocket(upkiSpiffeEndPoint);
            UPKIClient upkiClient = UPKIClientFactory.getUPKIClient(builder);
            this.upkiTokenService = new UPKITokenService(upkiClient);
        } catch (Exception e ) {
            LOG.error("Failed to configure upkiTokenService ",e);
        }
    }

    public static UPKITokenManager acquireUPKITokenManager() {
        synchronized (UPKITokenManager.class) {
            if (upkiTokenManagerInstance == null) {
                LOG.debug("initializing the UPKITokenManager");
                upkiTokenManagerInstance = new UPKITokenManager();
            }
            return upkiTokenManagerInstance;
        }
    }

    public UToken generateUToken() {
        UToken uToken = null;
        try {
            uToken = upkiTokenService.createSingleHop("piper");
            LOG.debug("utoken {}", uToken);
        } catch (UTokenException e) {
            LOG.error("Failed to create a utoken", e);
        }
        return uToken;
    }

}
