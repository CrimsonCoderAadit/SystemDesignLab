package com.systemdesign.lab5.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponseDto {

    private String id;
    private String registerNumber;
    private String name;
    private String department;
    private Integer year;
    private String email;
    private String storageNode;
}
