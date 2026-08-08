package com.systemdesign.lab5.controller;

import com.systemdesign.lab5.dto.AddNodeRequestDto;
import com.systemdesign.lab5.dto.MigrationStatsDto;
import com.systemdesign.lab5.migration.MigrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nodes")
public class NodeController {

    private final MigrationService migrationService;

    public NodeController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/add")
    public ResponseEntity<MigrationStatsDto> addNode(@Valid @RequestBody AddNodeRequestDto request) {
        MigrationStatsDto stats = migrationService.handleNodeAddition(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(stats);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<MigrationStatsDto> removeNode(@PathVariable String name) {
        MigrationStatsDto stats = migrationService.handleNodeRemoval(name);
        return ResponseEntity.ok(stats);
    }
}
