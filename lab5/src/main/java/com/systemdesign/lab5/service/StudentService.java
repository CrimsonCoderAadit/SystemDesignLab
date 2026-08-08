package com.systemdesign.lab5.service;

import com.systemdesign.lab5.dto.StudentRequestDto;
import com.systemdesign.lab5.dto.StudentResponseDto;
import com.systemdesign.lab5.exception.StudentNotFoundException;
import com.systemdesign.lab5.model.Student;
import com.systemdesign.lab5.repository.StudentRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentService {

    private final NodeManager nodeManager;
    private final StudentRepository studentRepository;

    public StudentService(NodeManager nodeManager, StudentRepository studentRepository) {
        this.nodeManager = nodeManager;
        this.studentRepository = studentRepository;
    }

    public StudentResponseDto createStudent(StudentRequestDto request) {
        String nodeName = nodeManager.resolveNode(request.getRegisterNumber());
        MongoTemplate mongoTemplate = nodeManager.getMongoTemplate(nodeName);

        Student student = Student.builder()
                .registerNumber(request.getRegisterNumber())
                .name(request.getName())
                .department(request.getDepartment())
                .year(request.getYear())
                .email(request.getEmail())
                .build();

        Student saved = studentRepository.save(mongoTemplate, student);
        return toResponseDto(saved, nodeName);
    }

    public StudentResponseDto getStudentById(String id) {
        for (String nodeName : nodeManager.getActiveNodeNames()) {
            MongoTemplate mongoTemplate = nodeManager.getMongoTemplate(nodeName);
            Optional<Student> found = studentRepository.findById(mongoTemplate, id);
            if (found.isPresent()) {
                return toResponseDto(found.get(), nodeName);
            }
        }
        throw new StudentNotFoundException("Student not found with id: " + id);
    }

    public List<StudentResponseDto> getAllStudents() {
        List<StudentResponseDto> allStudents = new ArrayList<>();
        for (String nodeName : nodeManager.getActiveNodeNames()) {
            MongoTemplate mongoTemplate = nodeManager.getMongoTemplate(nodeName);
            for (Student student : studentRepository.findAll(mongoTemplate)) {
                allStudents.add(toResponseDto(student, nodeName));
            }
        }
        return allStudents;
    }

    public Map<String, Long> getDistribution() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (String nodeName : nodeManager.getActiveNodeNames()) {
            distribution.put(nodeName, studentRepository.count(nodeManager.getMongoTemplate(nodeName)));
        }
        return distribution;
    }

    private StudentResponseDto toResponseDto(Student student, String nodeName) {
        return StudentResponseDto.builder()
                .id(student.getId())
                .registerNumber(student.getRegisterNumber())
                .name(student.getName())
                .department(student.getDepartment())
                .year(student.getYear())
                .email(student.getEmail())
                .storageNode(nodeName)
                .build();
    }
}
