package com.systemdesign.lab5.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageNode {

    private String name;

    private String host;

    private int port;

    private String database;
}
