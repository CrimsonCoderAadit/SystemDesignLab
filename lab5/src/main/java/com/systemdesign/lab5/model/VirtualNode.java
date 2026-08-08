package com.systemdesign.lab5.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualNode {

    private long hash;

    private String physicalNodeName;

    private int replicaIndex;
}
