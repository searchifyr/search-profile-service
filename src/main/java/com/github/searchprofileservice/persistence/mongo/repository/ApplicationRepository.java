package com.github.searchprofileservice.persistence.mongo.repository;

import com.github.searchprofileservice.persistence.mongo.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends MongoRepository<Application, UUID> {

    List<Application> findAll();

    List<Application> findAllByAllowedUserIdsContains(String userId);

    List<Application> findAllByCreatorId(String creatorId);

    Application getApplicationByApplicationName(String applicationName);

    Optional<Application> findOneByApplicationName(String applicationName);

    Boolean existsApplicationByApplicationName(String applicationName);
}
