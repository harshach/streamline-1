package com.hortonworks.streamline.common.credentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class CredentialsManagerImpl implements CredentialsManager {
    private Object yaml;

    public CredentialsManagerImpl() {
    }

    public void load(String configFile) {
        try(InputStream input = new FileInputStream(new File(configFile))) {
            yaml = new Yaml().load(input);
        } catch (IOException e) {
            throw new CredentialsManagerException(e);
        }
    }

    @Override
    public String getString(String name) {
        return (String) getValue(name);
    }

    /**
     * Get value by name. Name is of format key1.key2.key3
     */
    public Object getValue(String name) {
        List<String> parts = Arrays.asList(name.split("\\."));
        Object current = yaml;
        for (String part : parts) {
            current = ((Map<String, Object>) current).get(part);
        }
        return current;
    }
}
