package com.github.searchprofileservice.persistence.mongo.repository;

import com.github.searchprofileservice.persistence.mongo.model.SearchProfileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchProfileRepository extends MongoRepository<SearchProfileDocument, String> {

  List<SearchProfileDocument> findAll();

  /**
   * Retrieves all {@link SearchProfileDocument}s, only including their basic fields
   *
   * Basic fields are:
   * <ul>
   * <li> name
   * <li> profileId
   * <li> applicationId
   * <li> creatorId
   * <li> lastEditorId
   * <li> lastModifiedDate
   * </ul>
   *
   * @return the search profile documents
   */
  @Query(
    value="{}",
    fields="""
      {profileId: 1, applicationId : 1, creatorId: 1, lastEditorId: 1, lastModifiedDate: 1, name: 1}
    """
  )
  List<SearchProfileDocument> findAllAsBasicProjection();

  List<SearchProfileDocument> findAllByApplicationId(String applicationId);

  /**
   * Find all search profiles belonging to a specific application, returning only ther basic fields
   * 
   * @see {@link #findAllByApplicationId}
   * @see {@link #findAllAsBasicProjection}
   */
  @Query(
    value="{ 'applicationId': ?0 }",
    fields="""
      {profileId: 1, applicationId : 1, creatorId: 1, lastEditorId: 1, lastModifiedDate: 1, name: 1}
    """
  )
  List<SearchProfileDocument> findAllByApplicationIdAsBasicProjection(String applicationId);

  void deleteByApplicationId(String applicationId);

}
