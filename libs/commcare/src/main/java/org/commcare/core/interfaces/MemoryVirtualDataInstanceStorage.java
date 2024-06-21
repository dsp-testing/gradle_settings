package org.commcare.core.interfaces;

import org.javarosa.core.model.instance.ExternalDataInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In memory implementation of VirtualDataInstanceStorage for use in the CLI and tests.
 */
public class MemoryVirtualDataInstanceStorage implements
        VirtualDataInstanceStorage {

    private Map<String, ExternalDataInstance> storage = new HashMap<>();

    @Override
    public String write(ExternalDataInstance dataInstance) {
        String key = UUID.randomUUID().toString();
        storage.put(key, dataInstance);
        return key;
    }

    @Override
    public String write(String key, ExternalDataInstance dataInstance) {
        if (contains(key)) {
            throw new RuntimeException(String.format("Virtual instance with key '%s' already exists", key));
        }
        storage.put(key, dataInstance);
        return key;
    }

    @Override
    public ExternalDataInstance read(String key, String instanceId, String refId) {
        return storage.get(key);
    }

    @Override
    public boolean contains(String key) {
        return storage.containsKey(key);
    }

    public void clear() {
        storage.clear();
    }
}
