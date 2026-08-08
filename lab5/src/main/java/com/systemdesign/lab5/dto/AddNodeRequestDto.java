package com.systemdesign.lab5.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddNodeRequestDto {

    @NotBlank(message = "Node name is required")
    private String name;
}
