package com.github.searchprofileservice.persistence.mongo.repository;

import java.util.stream.Stream;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.github.searchprofileservice.persistence.mongo.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    User findByUserId(String userId);
    
    
    @Query(
        value="{activated: true}",
        fields=" {userId: 1, name : 1}"
    )
    Stream<User> findAllAsBasicProjection();
}
