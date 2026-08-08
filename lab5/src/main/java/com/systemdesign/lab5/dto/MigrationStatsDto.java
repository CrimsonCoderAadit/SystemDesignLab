package com.systemdesign.lab5.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationStatsDto {

    private String operation;
    private String affectedNode;
    private long totalRecordsMigrated;
    private Map<String, Long> migrationDetails;
    private long durationMillis;
}
