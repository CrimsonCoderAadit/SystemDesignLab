package com.systemdesign.lab5.migration;

import com.systemdesign.lab5.dto.MigrationStatsDto;
import com.systemdesign.lab5.exception.NodeNotFoundException;
import com.systemdesign.lab5.model.Student;
import com.systemdesign.lab5.repository.StudentRepository;
import com.systemdesign.lab5.service.NodeManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MigrationService {

    private final NodeManager nodeManager;
    private final StudentRepository studentRepository;

    public MigrationService(NodeManager nodeManager, StudentRepository studentRepository) {
        this.nodeManager = nodeManager;
        this.studentRepository = studentRepository;
    }

    public MigrationStatsDto handleNodeAddition(String nodeName) {
        long startTime = System.currentTimeMillis();

        nodeManager.addNode(nodeName);

        Map<String, Long> movedPerSourceNode = new LinkedHashMap<>();
        long totalMoved = 0;
        MongoTemplate targetTemplate = nodeManager.getMongoTemplate(nodeName);

        for (String sourceNode : nodeManager.getActiveNodeNames()) {
            if (sourceNode.equals(nodeName)) {
                continue;
            }

            MongoTemplate sourceTemplate = nodeManager.getMongoTemplate(sourceNode);
            List<Student> students = studentRepository.findAll(sourceTemplate);
            long movedFromThisNode = 0;

            for (Student student : students) {
                String resolvedNode = nodeManager.resolveNode(student.getRegisterNumber());
                if (resolvedNode.equals(nodeName)) {
                    studentRepository.save(targetTemplate, student);
                    studentRepository.deleteById(sourceTemplate, student.getId());
                    movedFromThisNode++;
                }
            }

            if (movedFromThisNode > 0) {
                movedPerSourceNode.put(sourceNode, movedFromThisNode);
            }
            totalMoved += movedFromThisNode;
        }

        return MigrationStatsDto.builder()
                .operation("ADD_NODE")
                .affectedNode(nodeName)
                .totalRecordsMigrated(totalMoved)
                .migrationDetails(movedPerSourceNode)
                .durationMillis(System.currentTimeMillis() - startTime)
                .build();
    }

    public MigrationStatsDto handleNodeRemoval(String nodeName) {
        long startTime = System.currentTimeMillis();

        if (!nodeManager.isActive(nodeName)) {
            throw new NodeNotFoundException("Node is not active: " + nodeName);
        }

        MongoTemplate removedTemplate = nodeManager.getMongoTemplate(nodeName);
        List<Student> studentsToMigrate = studentRepository.findAll(removedTemplate);

        nodeManager.removeNode(nodeName);

        Map<String, Long> movedPerTargetNode = new LinkedHashMap<>();
        long totalMoved = 0;

        for (Student student : studentsToMigrate) {
            String targetNodeName = nodeManager.resolveNode(student.getRegisterNumber());
            MongoTemplate targetTemplate = nodeManager.getMongoTemplate(targetNodeName);
            studentRepository.save(targetTemplate, student);
            movedPerTargetNode.merge(targetNodeName, 1L, Long::sum);
            totalMoved++;
        }

        studentRepository.deleteAll(removedTemplate);

        return MigrationStatsDto.builder()
                .operation("REMOVE_NODE")
                .affectedNode(nodeName)
                .totalRecordsMigrated(totalMoved)
                .migrationDetails(movedPerTargetNode)
                .durationMillis(System.currentTimeMillis() - startTime)
                .build();
    }
}
