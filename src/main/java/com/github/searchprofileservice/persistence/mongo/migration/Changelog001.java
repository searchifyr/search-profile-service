package com.github.searchprofileservice.persistence.mongo.migration;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.persistence.mongo.repository.ApplicationRepository;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MongoDB migration
 * 
 * Enforces unique application names by renaming applications w/ duplicate names and enabling
 * a mongodb index to ensure application names are unique.
 */
@ChangeLog(order = "001")
public final class Changelog001 {

  /**
   * Renames applications so that all application names are unique
   */
  @ChangeSet(order = "001", id = "renames_duplicate_application_names", author = "anonymous")
  public void makeApplicationNamesUnique(ApplicationRepository applicationRepository) {
    applicationRepository.findAll()
      .stream()
      // find collections of applications with same name
      .<Map<String, List<Application>>>reduce(
        new HashMap<>(),
        (map, application) -> {
          final String applicationName = application.getApplicationName();

          if (map.containsKey(applicationName)) {
            map.get(applicationName).add(application);
          } else {
            final List<Application> applicationList = new ArrayList<>();
            applicationList.add(application);
            map.put(applicationName, applicationList);
          }

          return map;
        },
        // Merge maps (in case of parallel folding)
        (a, b) -> {
          for (var entry : b.entrySet()) {
            if (a.containsKey(entry.getKey())) {
              a.get(entry.getKey()).addAll(entry.getValue());
            } else {
              a.put(entry.getKey(), entry.getValue());
            }
          }
          return a;
        })
        .entrySet()
        .stream()
        // filter applications with already unique name
        .filter(entry -> entry.getValue().size() > 1)
        // rename applications
        .map(entry -> {
          final AtomicInteger i = new AtomicInteger(1);
          return
            entry.getValue().stream()
              .map(application -> {
                application.setApplicationName(entry.getKey() + " (" + i.getAndIncrement() + ")");
                return application;
              })
              .toList();
        })
        // save updated applications
        .forEach(applicationRepository::saveAll);
  }

  /**
   * Enables Mongo indices on Application.class
   */
  @ChangeSet(order = "002", id = "enables_application_name_unique_index", author = "anonymous")
  public void enableApplicationNameUniqueIndex(MongockTemplate mongoTemplate) {
    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext =
      mongoTemplate.getConverter().getMappingContext();

    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    IndexOperations indexOps = mongoTemplate.indexOps(Application.class);
    resolver.resolveIndexFor(Application.class).forEach(indexOps::ensureIndex);
  }

}
