package com.systemdesign.lab5.hashing;

import com.systemdesign.lab5.config.HashRingProperties;
import com.systemdesign.lab5.model.VirtualNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Component
public class ConsistentHashRing {

    private final NavigableMap<Long, VirtualNode> ring = new ConcurrentSkipListMap<>();
    private final int virtualNodesPerPhysicalNode;

    public ConsistentHashRing(HashRingProperties hashRingProperties) {
        this.virtualNodesPerPhysicalNode = hashRingProperties.getVirtualNodes();
    }

    public synchronized void addNode(String physicalNodeName) {
        for (int replicaIndex = 0; replicaIndex < virtualNodesPerPhysicalNode; replicaIndex++) {
            String virtualNodeKey = physicalNodeName + "#VN" + replicaIndex;
            long hash = HashUtil.sha256Hash(virtualNodeKey);
            ring.put(hash, VirtualNode.builder()
                    .hash(hash)
                    .physicalNodeName(physicalNodeName)
                    .replicaIndex(replicaIndex)
                    .build());
        }
    }

    public synchronized void removeNode(String physicalNodeName) {
        ring.entrySet().removeIf(entry -> entry.getValue().getPhysicalNodeName().equals(physicalNodeName));
    }

    public String getResponsibleNode(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring is empty, no storage nodes available");
        }
        long hash = HashUtil.sha256Hash(key);
        Map.Entry<Long, VirtualNode> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return entry.getValue().getPhysicalNodeName();
    }

    public List<VirtualNode> getOrderedVirtualNodes() {
        return new ArrayList<>(ring.values());
    }

    public Set<String> getPhysicalNodes() {
        return ring.values().stream()
                .map(VirtualNode::getPhysicalNodeName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean containsNode(String physicalNodeName) {
        return ring.values().stream().anyMatch(v -> v.getPhysicalNodeName().equals(physicalNodeName));
    }

    public int size() {
        return ring.size();
    }
}
