package com.systemdesign.lab5.repository;

import com.systemdesign.lab5.model.Student;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {

    private static final String COLLECTION = "students";

    public Student save(MongoTemplate mongoTemplate, Student student) {
        return mongoTemplate.save(student, COLLECTION);
    }

    public Optional<Student> findById(MongoTemplate mongoTemplate, String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Student.class, COLLECTION));
    }

    public List<Student> findAll(MongoTemplate mongoTemplate) {
        return mongoTemplate.findAll(Student.class, COLLECTION);
    }

    public long count(MongoTemplate mongoTemplate) {
        return mongoTemplate.getCollection(COLLECTION).countDocuments();
    }

    public void deleteById(MongoTemplate mongoTemplate, String id) {
        Query query = Query.query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, Student.class, COLLECTION);
    }

    public void deleteAll(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), Student.class, COLLECTION);
    }
}
