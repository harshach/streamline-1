package com.hortonworks.streamline.common.credentials;

/**
 * Interface for retrieving credentials
 */
public interface CredentialsManager {

    /**
     *  Loads the credentials from the given file
     */
    void load(String credentialsFile) throws CredentialsManagerException;

    /**
     * Retrieve a String based credential
     */
    String getString(String key) throws CredentialsManagerException;
}
