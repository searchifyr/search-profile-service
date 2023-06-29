package com.github.searchprofileservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.persistence.mongo.repository.ApplicationRepository;
import com.github.searchprofileservice.service.ApplicationService;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import com.github.searchprofileservice.service.SearchProfileService;
import javassist.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final SearchProfileService searchProfileService;
  private final ElasticSearchClientService elasticSearchService;
  private final AuthenticationService authenticationService;

  @Override
  public List<Application> findAll() {
    return applicationRepository.findAll();
  }

  @Override
  public List<Application> findAllByAllowedUserIdsContains (String userId) {
    return applicationRepository.findAllByAllowedUserIdsContains(userId);
  }

  @Override
  public List<Application> findAllByUserId(String userId) {
    return applicationRepository.findAllByCreatorId(userId);
  }

  @Override
  public boolean existsById(UUID id) {
    return applicationRepository.existsById(id);
  }

  @Override
  public Optional<Application> findById(UUID id) {
    return applicationRepository.findById(id);
  }

  @Override
  public Optional<Application> save(Application application) {

    if (StringUtils.isBlank(application.getApplicationName())) {
      throw new IllegalArgumentException("'applicationName' can not be null or empty");
    }

    if (StringUtils.isBlank(application.getCreatorId())) {
      throw new IllegalArgumentException("'creatorId' can not be null or empty");
    }

    if (null == application.getId()) {
      application.setId(UUID.randomUUID());
    }

    if(applicationIsRedundant(application))
      return Optional.empty();

    return saveApplication(application);
  }

  private Optional<Application> saveApplication(Application application){

      if(!elasticSearchService.createIndex(application))
        return Optional.empty();

    return Optional.of(applicationRepository.save(application));
  }

  @Override
  public Application update(Application application) {
    if (null == application.getId()) {
      throw new IllegalArgumentException("'id' can not be null");
    }

    if (null == application.getApiKeys()) {
      throw new IllegalArgumentException("'apiKeys' can not be null");
    }

    if (StringUtils.isBlank(application.getApplicationName())) {
      throw new IllegalArgumentException("'applicationName' can not be null or empty");
    }

    if (StringUtils.isBlank(application.getCreatorId())) {
      throw new IllegalArgumentException("'creatorId' can not be null or empty");
    }

    if (applicationIsRedundant(application)) {
      throw new IllegalArgumentException("applicationName must be unique");
    }

    return applicationRepository.save(application);
  }

  @Override
  public String uploadDocument(String rawJson, UUID applicationId) throws IOException {
    var application = applicationRepository.findById(applicationId);
    if (!application.isEmpty()) {
      setApplicationActivity(application.get());
      return elasticSearchService.uploadRawJsonToApplication(applicationId, rawJson);
    } else {
      throw new IllegalArgumentException("Invalid application id '" + applicationId + '"');
    }
  }

  @Override
  public void updateDocument(
    String rawJson,
    UUID applicationId,
    String documentId
  ) throws IOException {
    if (this.existsById(applicationId)) {
      elasticSearchService.updateDocument(applicationId, documentId, rawJson);
    } else {
      throw new IllegalArgumentException("Invalid application id '" + applicationId + '"');
    }
  }

  @Override
  public void deleteById(UUID id) throws IOException {
    elasticSearchService.deleteIndex(id); // throws if index can not be deleted
    applicationRepository.deleteById(id);
    searchProfileService.deleteByApplicationId(id);
  }

  @Override
  public boolean isJsonValid(String rawJson) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      mapper.readTree(rawJson);
      return true;
    } catch (IOException e) {
      log.warn(e.getMessage());
      return false;
    }
  }

  @Override
  public Map<String, ElasticSearchMappingType> retrieveIndexMappingByApplicationId(UUID id) {
    return new TreeMap<>(elasticSearchService.getIndexMapping(id.toString()));
  }

  @Override
  public List<String> bulkUploadDocuments(String bulkJson, UUID applicationId) throws IOException{
    List<String> jsonDocuments = getJsonDataFromBulkDocument(bulkJson);

   var application = applicationRepository.findById(applicationId);
    if (!application.isEmpty()) {
      setApplicationActivity(application.get());
      return elasticSearchService.bulkUploadRawJsonToApplication(applicationId, jsonDocuments);
    } else {
     throw new IllegalArgumentException("Invalid application id '" + applicationId + '"');
    }
  }

  private List<String> getJsonDataFromBulkDocument(String bulkJson) throws JSONException{
      List<String> jsonData = new ArrayList<>();
      JSONObject obj = new JSONObject(bulkJson);
      JSONArray arr = obj.getJSONArray("Documents");
      for (int i = 0; i < arr.length(); i++)
      {
        String rawJson = arr.getJSONObject(i).toString();
        jsonData.add(rawJson);
      }
      return jsonData;
  }

  private boolean applicationIsRedundant(Application application) {
    return applicationRepository.findOneByApplicationName(application.getApplicationName())
      .map(foundApplication -> !foundApplication.getId().equals(application.getId()))
      .orElse(false);
  }

  private void setApplicationActivity (Application application){
    if (!application.isActive()) {
      application.setActive(true);
      update(application);
    }
    return;
  }

  @Override
  public boolean isEditableByCurrentUser(Application application) {
    return wasCreatedByCurrentUser(application);
  }

  @Override
  public boolean wasCreatedByCurrentUser(Application application) {
    var currentUserId = authenticationService.getUser().getId();
    if (application.getCreatorId().equals(currentUserId))
      return true;
    if (application.getAllowedUserIds() != null && application.getAllowedUserIds().contains(currentUserId))
      return true;
    else return false;
  }

   @Override
  public void addNewApiKeyToApp(Application application, ApiKey newApiKey) {
    application.addApiKey(newApiKey);
    update(application);
  }

  @Override
  public void deleteApiKeyFromApp(Application application, UUID apiKeyId) throws NotFoundException {
    Optional<ApiKey> apiKeyToDelete = getApiKeyFromApplication(application, apiKeyId);
    if (apiKeyToDelete.isPresent()) {
      application.removeApiKey(apiKeyToDelete.get());
      update(application);
    }
    else throw new NotFoundException("There is no api Key with the given Id");
  }

  private static Optional<ApiKey> getApiKeyFromApplication(Application application, UUID apiKeyId) {
    return application.getApiKeys().stream().filter(key -> key.getId().equals(apiKeyId)).findFirst();
  }


}
