package com.github.searchprofileservice.service;

import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import javassist.NotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for interacting with {@link com.github.searchprofileservice.persistence.mongo.model.Application}s
 */
public interface ApplicationService {

  /**
   * @return all applications
   */
  List<Application> findAll();

  /**
   * @return all applications the user is allowed for
   */
  List<Application> findAllByAllowedUserIdsContains(String userId);

  /**
   * @param userId The id of the user
   * @return all applications
   */
  List<Application> findAllByUserId(String userId);

  /**
   * @param id the id to lookup
   * @return `true`, if an application w/ given id exists, `false` otherwise
   */
  boolean existsById(UUID id);

  /**
   * @param id the id to lookup
   * @return Optional of application, if an application w/ given id exists, empty optional otherwise
   */
  Optional<Application> findById(UUID id);

  /**
   * Saves a *new* `Application` and adds a standard ApiKey and id if not present
   * @param application the application to save
   * @return the *created* application
   * 
   * @throws IllegalArgumentException if application is missing mandatory fields
   */
  Optional<Application> save(Application application);

  /**
   * Updates an *existing* `Application`
   * @param application the application to update
   * @return the updated application
   *
   * @throws IllegalArgumentException if application is missing mandatory fields
   */
  Application update(Application application);

  /**
   * Uploads a json document into an application's es index
   * @param applicationId the id of the application whose es-index to insert the data into
   * @param rawJson document to insert
   * @return The id of the created document
   */
  String uploadDocument(String rawJson, UUID applicationId) throws IOException;


  /**
   * Updates a json document residing in an application's es index
   * @param rawJson document to insert
   * @param applicationId the id of the application whose es-index the document belongs to
   * @param documentId the id of es-document to update
   *
   * @throws IOException if document manipulation fails
   */
  void updateDocument(String rawJson, UUID applicationId, String documentId) throws IOException;

  /**
   * Deletes an application
   * @param id the id of the application to remove
   * @throws IOException
   */
  void deleteById(UUID id) throws IOException;

  /**
   * Validates a string if it is a valid JSON Document
   * @param rawJson raw Json Document
   * @return either Json document is satisfy Json standard or not
   */
  boolean isJsonValid(String rawJson);

  /**
   *
   * @param id The id of the application.
   * @return The mapping of the index with the fields being ordered alphabetically.
   */
  Map<String, ElasticSearchMappingType> retrieveIndexMappingByApplicationId(UUID id);

  /**
   * Bulk Upload multiple Json documents to an existing application/elasticsearch index
   *
   * @param bulkJson Json Document as String. Contains an Array of Json Documents
   * @param applicationId ApplicationId where the Json Documents are going to be uploaded
   * @return List of UUIDs which represent the successful uploaded Documents
   * @throws IOException
   */
  List<String> bulkUploadDocuments(String bulkJson, UUID applicationId) throws IOException;

  /**
   * Checks, whether the accessed app was created by the current user or if the user is allowed to use it
   *
   * @param application the application, for which to verify the access rights
   * @return true, if the app is created by the current user or the current user is allowed to use it
   * false, if not
   */
  boolean isEditableByCurrentUser(Application application);

  /**
   * Checks, whether the accessed app was created by the current user
   *
   * @param application the application, which to verify the access rights
   * @return true, if the app was created by the current user
   * false, if not
   */
  boolean wasCreatedByCurrentUser(Application application);

  /**
   * Adds a new api key for a specified application with a given name
   *
   * @param application The application to which to add the new api key to
   * @param newApiKey   the encrypted api key to be added to the application
   */
  void addNewApiKeyToApp(Application application, ApiKey newApiKey);

  /**
   * @param application the application to delete the apiKey from
   * @param apiKeyId the id of the apiKey to delete
   * @throws javassist.NotFoundException if apiKey with the given id is not present in the application
   */
  void deleteApiKeyFromApp(Application application, UUID apiKeyId) throws NotFoundException;
}
