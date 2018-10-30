package com.hortonworks.streamline.common.credentials;

/**
 * Factory class for retrieving correct implementation of CredentialsManager
 */
public class CredentialsManagerFactory {

    /**
     * Get correct implementation of credentials manager
     */
    public static CredentialsManager getSecretsManager() throws CredentialsManagerException {
        return new CredentialsManagerImpl();
    }
}
