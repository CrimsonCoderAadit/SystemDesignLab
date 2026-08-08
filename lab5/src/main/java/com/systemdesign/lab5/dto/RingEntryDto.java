package com.systemdesign.lab5.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RingEntryDto {

    private String hash;
    private String physicalNode;
    private int replicaIndex;
}
