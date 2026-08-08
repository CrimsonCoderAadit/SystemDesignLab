package com.systemdesign.lab5.service;

import com.systemdesign.lab5.config.HashRingProperties;
import com.systemdesign.lab5.config.MongoNodeProperties;
import com.systemdesign.lab5.exception.InvalidNodeOperationException;
import com.systemdesign.lab5.exception.NodeAlreadyExistsException;
import com.systemdesign.lab5.exception.NodeNotFoundException;
import com.systemdesign.lab5.hashing.ConsistentHashRing;
import com.systemdesign.lab5.model.StorageNode;
import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NodeManager {

    private final ConsistentHashRing hashRing;
    private final MongoNodeProperties mongoNodeProperties;
    private final HashRingProperties hashRingProperties;
    private final Map<String, MongoTemplate> mongoTemplateMap;
    private final Map<String, StorageNode> activeNodes = new ConcurrentHashMap<>();

    public NodeManager(ConsistentHashRing hashRing,
                        MongoNodeProperties mongoNodeProperties,
                        HashRingProperties hashRingProperties,
                        Map<String, MongoTemplate> mongoTemplateMap) {
        this.hashRing = hashRing;
        this.mongoNodeProperties = mongoNodeProperties;
        this.hashRingProperties = hashRingProperties;
        this.mongoTemplateMap = mongoTemplateMap;
    }

    @PostConstruct
    public void init() {
        for (String nodeName : hashRingProperties.getInitialNodes()) {
            StorageNode node = buildStorageNode(nodeName);
            hashRing.addNode(nodeName);
            activeNodes.put(nodeName, node);
        }
    }

    private StorageNode buildStorageNode(String name) {
        MongoNodeProperties.NodeConfig config = mongoNodeProperties.getNodes().get(name);
        if (config == null) {
            throw new NodeNotFoundException("No Mongo configuration found for node: " + name);
        }
        return StorageNode.builder()
                .name(name)
                .host(config.getHost())
                .port(config.getPort())
                .database(config.getDatabase())
                .build();
    }

    public StorageNode addNode(String name) {
        if (activeNodes.containsKey(name)) {
            throw new NodeAlreadyExistsException("Node is already active: " + name);
        }
        StorageNode node = buildStorageNode(name);
        hashRing.addNode(name);
        activeNodes.put(name, node);
        return node;
    }

    public void removeNode(String name) {
        if (!activeNodes.containsKey(name)) {
            throw new NodeNotFoundException("Node is not active: " + name);
        }
        if (activeNodes.size() <= 1) {
            throw new InvalidNodeOperationException("Cannot remove the last remaining storage node");
        }
        hashRing.removeNode(name);
        activeNodes.remove(name);
    }

    public MongoTemplate getMongoTemplate(String nodeName) {
        MongoTemplate template = mongoTemplateMap.get(nodeName);
        if (template == null) {
            throw new NodeNotFoundException("No MongoTemplate configured for node: " + nodeName);
        }
        return template;
    }

    public Set<String> getActiveNodeNames() {
        return Collections.unmodifiableSet(activeNodes.keySet());
    }

    public Collection<StorageNode> getActiveNodes() {
        return Collections.unmodifiableCollection(activeNodes.values());
    }

    public String resolveNode(String key) {
        return hashRing.getResponsibleNode(key);
    }

    public boolean isActive(String name) {
        return activeNodes.containsKey(name);
    }
}
