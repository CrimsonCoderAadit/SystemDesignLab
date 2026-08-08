package com.systemdesign.lab5.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RingViewDto {

    private int totalVirtualNodes;
    private List<String> physicalNodes;
    private List<RingEntryDto> virtualNodes;
}
