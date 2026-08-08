package com.systemdesign.lab5.controller;

import com.systemdesign.lab5.dto.RingEntryDto;
import com.systemdesign.lab5.dto.RingViewDto;
import com.systemdesign.lab5.hashing.ConsistentHashRing;
import com.systemdesign.lab5.service.NodeManager;
import com.systemdesign.lab5.util.HashFormatter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class RingController {

    private final ConsistentHashRing hashRing;
    private final NodeManager nodeManager;

    public RingController(ConsistentHashRing hashRing, NodeManager nodeManager) {
        this.hashRing = hashRing;
        this.nodeManager = nodeManager;
    }

    @GetMapping("/ring")
    public ResponseEntity<RingViewDto> getRing() {
        List<RingEntryDto> virtualNodes = hashRing.getOrderedVirtualNodes().stream()
                .map(vn -> RingEntryDto.builder()
                        .hash(HashFormatter.toHex(vn.getHash()))
                        .physicalNode(vn.getPhysicalNodeName())
                        .replicaIndex(vn.getReplicaIndex())
                        .build())
                .collect(Collectors.toList());

        RingViewDto ringView = RingViewDto.builder()
                .totalVirtualNodes(virtualNodes.size())
                .physicalNodes(new ArrayList<>(nodeManager.getActiveNodeNames()))
                .virtualNodes(virtualNodes)
                .build();

        return ResponseEntity.ok(ringView);
    }
}
