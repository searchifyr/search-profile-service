package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.model.*;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.User;
import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.*;
import javassist.NotFoundException;
import java.io.IOException;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.compress.NotXContentException;
import org.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.searchprofileservice.api.routes.Routes.Api.V1.Applications.*;


/**
 * Endpoint for CRUD of {@link com.github.searchprofileservice.persistence.mongo.model.Application}
 *
 * <p>Uses {@link ApplicationDto} as exchange object between.
 * server and client.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@Secured("ROLE_USER")
public class ApplicationController {

  private final ApplicationService applicationService;
  private final SearchProfileService searchProfileService;
  private final AuthenticationService authenticationService;
  private final ApplicationConverterService applicationConverter;
  private final UserService userService;


  /**
   * Returns all existing applications created by the user or the ones the user is allowed to use.
   *
   * @param ownApplications If true, only the applications which the user created are returned, if false, any application for which the user is approved will be returned
   *
   * @return
   *  200, if the search was successful
   */
  @GetMapping(path = GetAll)
  public ResponseEntity<List<ApplicationDto>> getAllApplications(
      @RequestParam(name = "ownApplications") boolean ownApplications) {

    //ownApplications = true
    if (ownApplications) {
      List<ApplicationDto> applicationDTOs = applicationService
              .findAllByUserId(authenticationService.getUser().getId())
              .stream()
              .map(app -> applicationConverter.convertToApplicationDto(app))
              .toList();
      return ResponseEntity.ok(applicationDTOs);
    }

    //ownApplications = false
    else {
      List<ApplicationDto> applicationDTOs = applicationService
              .findAllByAllowedUserIdsContains(authenticationService.getUser().getId())
              .stream()
              .map(app -> applicationConverter.convertToApplicationDto(app))
              .toList();
      return ResponseEntity.ok(applicationDTOs);
    }
  }

  /**
   * Returns all search profiles for given application id.
   *
   * @param applicationId of the application the corresponding search-profiles the endpoint is supposed to get
   * @return
   *  200, if successfully retrieved the search-profiles
   *  400, if id is not a valid UUID
   *  404, if there is no application for given application id
   */
  @GetMapping(path = GetOne.GetSearchProfiles)
  public ResponseEntity<List<SearchProfileDto.BasicProjection>> getAllSearchProfilesBy(
      @PathVariable(GetOne.PathParams.applicationId) String applicationId) {

    final UUID uuid =
            getUuidFromString(applicationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "'" + applicationId + "' is not a valid UUID"));

    if (!applicationService.findById(uuid).isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no application for given ID!");

    List<SearchProfileDto.BasicProjection> searchProfiles =
        searchProfileService.getAllSearchProfilesByApplicationIdAsBasicProjection(applicationId);
    return ResponseEntity.ok(searchProfiles);
  }

  /**
   * Returns one application for given applicationId
   *
   * @return
   *  200, if application was found successfully
   *  400, if id is not a valid UUID
   *  404, if no application exists for the given id
   */
  @GetMapping(path = GetOne.route)
  public ResponseEntity<ApplicationDto> getApplication (
      @PathVariable(GetOne.PathParams.applicationId) String id
  ) {
    final UUID uuid =
        getUuidFromString(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "'" + id + "' is not a valid UUID"));

    Optional<ApplicationDto> applicationDto =
        applicationService.findById(uuid).map(app -> applicationConverter.convertToApplicationDto(app));
    if (applicationDto.isPresent()) {
      return ResponseEntity.ok(applicationDto.get());
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "No application could be found for the given id.");
    }
  }

  /**
   * Creates a new application.
   *
   * <p>Creates id and apiKey of application before storing application
   *
   * @param applicationDto the application to create
   * @return
   *  201 and the application with its initial api key on success
   *  400, if the application-name is redundant or User-definable fields are empty/null or missing
   */
  @PostMapping(path = Post)
  public ResponseEntity<ApplicationCreatedResponse> createApplication(
      @RequestBody ApplicationDto applicationDto) {

    if (StringUtils.isBlank(applicationDto.getName())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "'name' must not be null or empty.");
    }

    Application newApplication = applicationConverter.convertToApplication(applicationDto);
    newApplication.setActive(false); // new applications are always inactive
    newApplication.setCreatorId(authenticationService.getUser().getId());

    String clearTextApiKey = UUID.randomUUID().toString();
    final String salt = BCrypt.gensalt(14);
    String encryptedApiKey = encryptApiKey(clearTextApiKey, salt);

    newApplication.addApiKey(new ApiKey(UUID.randomUUID(), "Standard", encryptedApiKey));

    final Optional<Application> application = applicationService.save(newApplication);

    if (application.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Creation of application failed. Application is redundant.");
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new ApplicationCreatedResponse(clearTextApiKey,
                    applicationConverter.convertToApplicationDto(application.get())));
  }

  private static String encryptApiKey(String apikey, String salt) {
    return BCrypt.hashpw(apikey, salt);
  }

  /**
   * Describes the result of inserting a new document into elastic search.
   * Encapsulates the document's id.
   */
  public record DocumentCreateResult(String documentId) { }

  /**
   * Uploads a JSON document into the elastic search index of an application
   * @param json the document to upload
   * @param applicationIdString the id of the application into whose index to insert the document
   * @return the result of the insertion
   */
  @PostMapping(path = GetOne.PostDocument)
  public ResponseEntity<DocumentCreateResult> uploadDocument(
    @RequestBody String json,
    @PathVariable(GetOne.PathParams.applicationId) String applicationIdString
  ) {
    Optional<UUID> applicationId = getUuidFromString(applicationIdString);

    if (StringUtils.isBlank(json)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' must not be null or empty.");
    } else if (StringUtils.isBlank(applicationIdString)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'applicationId' must not be null or empty.");
    } else if (applicationId.isEmpty()) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
        "applicationId '" + applicationId + "' must be a valid UUID");
    } else if(!applicationService.isJsonValid(json)){
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' is not satisfying Json standard");
    }

    Application application = applicationService.findById(applicationId.get()).get();
    if (!applicationService.isEditableByCurrentUser(application)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have not created this application or you are not allowed to use it.");
    }

    try {
      String documentId = applicationService.uploadDocument(json, applicationId.get());
      return ResponseEntity
          .status(HttpStatus.CREATED)

          .body(new DocumentCreateResult(documentId));
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indexing Json Failed.");
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "applicationId '" + applicationId + "' is not a valid application id");
    }
  }

  /**
   * Updates a JSON document inside elastic search or creates a new one
   * @param json the data to update the document with
   * @param applicationIdString the id of the application to which the document belongs
   * @param documentId the id of the document to update
   */
  @PutMapping(path = GetOne.PutDocument.route)
  public ResponseEntity<Void> updateDocument(
    @RequestBody String json,
    @PathVariable(GetOne.PathParams.applicationId) String applicationIdString,
    @PathVariable(GetOne.PutDocument.PathParams.documentId) String documentId
  ) {
    Optional<UUID> applicationId = getUuidFromString(applicationIdString);

    if (StringUtils.isBlank(json)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' must not be null or empty");
    } else if (applicationId.isEmpty()) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "applicationId '" + applicationId + "' must be a valid UUID");
    } else if (StringUtils.isBlank(documentId)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'DocumentId' must not be null or empty");
    }

    Application application = applicationService.findById(applicationId.get()).get();
    if (!applicationService.isEditableByCurrentUser(application)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have not created this application or you are not allowed to use it.");
    }

    try {
      applicationService.updateDocument(json, applicationId.get(), documentId);
      return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Indexing Json Failed.");
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "applicationId '" + applicationId + "' is not a valid application id");
    }
  }

  /**
   * Updates an existing application.
   *
   * @param id of the application to update
   * @param applicationDto the updated application
   * @return
   *  200 on success
   *  400, if the given id is not a valid UUID or there are missing fields or given UserIds are invalid
   *  404, if there is no existing application matching given application id
   */
  @PutMapping(path = Put.route)
  public ResponseEntity<ApplicationDto> updateApplication(
      @PathVariable(Put.PathParams.applicationId) String id,
      @RequestBody ApplicationDto applicationDto) {

    if (StringUtils.isBlank(applicationDto.getName())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'name' must not be null or empty");
    }

    final UUID uuid =
            getUuidFromString(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "'" + id + "' is not a valid UUID"));

    Optional<Application> maybeApplication = applicationService.findById(uuid);

    if (maybeApplication.isPresent()) {

      if (!applicationService.isEditableByCurrentUser(maybeApplication.get())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have not created this application or you are not allowed to use it.");
      }

        if (applicationDto.getAllowedUserIds() != null) {
          for (int i = 0; i < applicationDto.getAllowedUserIds().size(); i++) {
            if (StringUtils.isBlank(applicationDto.getAllowedUserIds().get(i))) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only allow Users with a valid UserId access to your application!");
            }
          }

          for (int i = 0; i < applicationDto.getAllowedUserIds().size(); i++) {
            for (int j = 0; j < applicationDto.getAllowedUserIds().size(); j++) {
              if ((i != j) && applicationDto.getAllowedUserIds().get(i).equals(applicationDto.getAllowedUserIds().get(j))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only allow a user once for an application!");
              }
            }
          }

          List<String> userIds = userService
                  .getAllUsers()
                  .stream()
                  .map(User::getUserId)
                  .toList();
          for (int i = 0; i < applicationDto.getAllowedUserIds().size(); i++) {
            if (!userIds.contains(applicationDto.getAllowedUserIds().get(i))) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user found with given in AllowedUserIds given UserId (" + applicationDto.getAllowedUserIds().get(i) + ").");
            }
          }
        }

        Application application = maybeApplication.get();
        application.setApplicationName(applicationDto.getName());
        if (applicationDto.getAllowedUserIds() != null) {
          application.setAllowedUserIds(applicationDto.getAllowedUserIds());
        } else
          application.setAllowedUserIds(Collections.emptyList());

        try {
          application = applicationService.update(application);
          return ResponseEntity.ok(applicationConverter.convertToApplicationDto(application));
        } catch (IllegalArgumentException e) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
      } else {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No application could be found for the given id.");
      }
  }

  /**
   * Deletes an existing application.
   *
   * @param id the id of the application to delete
   * @return
   *  204 on success
   *  400 on ids which are no valid UUIDs
   *  404 on non-existing application to given Id
   *  500, if the cooresponding elastic search index can't be deleted
   */
  @DeleteMapping(path = Delete.route)
  public ResponseEntity<Void> deleteApplication(
      @PathVariable(Put.PathParams.applicationId) String id
  ) {
    final UUID uuid =
        getUuidFromString(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "'" + id + "' is not a valid UUID"));

    if (!applicationService.existsById(uuid)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "No application exists for the given id");
    }

    Application application = applicationService.findById(uuid).get();
    if (!applicationService.isEditableByCurrentUser(application)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have not created this application or you are not allowed to use it.");
    }

    try {
      applicationService.deleteById(uuid);
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Error while deleting application '" + uuid + "'");
    }

    return ResponseEntity.noContent().build();
  }

  /**
   *  Returns the mapping of the application index in elasticsearch.
   *
   * @param id The id of the application.
   * @return
   *  200 on success
   *  404 on non-existing application index
   */
  @GetMapping(path = GetOne.GetMapping)
  public ResponseEntity<Map<String, ElasticSearchMappingType>> getIndexMappingOfApplication(
      @PathVariable(Put.PathParams.applicationId) String id
  ) {

    UUID applicationId = getUuidFromString(id).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "'" + id + "' is not a valid UUID"));

    return ResponseEntity.ok(applicationService.retrieveIndexMappingByApplicationId(applicationId));
  }

  /**
   * Upload multiple Json Documents to an existing ElasticSearch Application/Index
   * @param bulkJson Json File which contains multiple Json Documents for uploading to an ElasticSearch Application
   * @param applicationIdString the id of the application to which the documents belong
   * @return HTTP 200 on success, HTTP 400 on failure
   */
  @PostMapping(path = GetOne.PostBulkUpload)
  public ResponseEntity<List<String>> bulkUploadDocuments(
          @RequestBody String bulkJson,
          @PathVariable(GetOne.PathParams.applicationId) String applicationIdString) {
    Optional<UUID> applicationId = getUuidFromString(applicationIdString);

    if (StringUtils.isBlank(bulkJson)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request-body must not be null or empty");
    } else if (!applicationService.isJsonValid(bulkJson)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request-body is not satisfying Json standard");
    } else if (StringUtils.isBlank(applicationIdString)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'ApplicationId' must not be null or empty");
    } else if (applicationId.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "applicationId '" + applicationId + "' must be a valid UUID");
    }

    Application application = applicationService.findById(applicationId.get()).get();
    if (!applicationService.isEditableByCurrentUser(application)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have not created this application or you are not allowed to use it.");
    }

    try {
     List<String> successfulDocIds = applicationService.bulkUploadDocuments(bulkJson, applicationId.get());
      return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(successfulDocIds);
    } catch (IOException | NotXContentException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indexing Jsons Failed.");
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "applicationId '" + applicationId + "' is not a valid application id");
    } catch (JSONException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Documents are not satisfying Json standard");
    }
  }

  /**
   * Creates a new api key for an application. Only this method returns the full api key,
   * so it should be saved for later use by the user
   * @param appId the UUID of the application the new api key is generated for
   * @param name the name the new api key is supposed to have
   * @return
   *  201, if successfully created plus the ApiKeyDto object and a message with the full actual api key
   *  400, if there are missing/invalid parameters
   */
  @PostMapping(path = GetOne.PostApiKey)
  public ResponseEntity<ApiKeyCreatedResponse> createNewApiKeyForAppWithId (
          @PathVariable(GetOne.PathParams.applicationId) String appId,
          @RequestBody String name) {
    if (StringUtils.isBlank(name)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "'name' must not be null or empty.");
    }

    Optional<Application> optionalApplication = applicationService.findById(UUID.fromString(appId));

    if (optionalApplication.isPresent()) {

      if (!applicationService.isEditableByCurrentUser(optionalApplication.get())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have not created this application or you are not allowed to use it.");
      }

      Application application = optionalApplication.get();

      String clearTextApiKey = UUID.randomUUID().toString();
      String encryptedKey = encryptApiKey(clearTextApiKey, BCrypt.gensalt(14));
      ApiKey newApiKey = new ApiKey(UUID.randomUUID(), name, encryptedKey);
      applicationService.addNewApiKeyToApp(application, newApiKey);

      return ResponseEntity.status(HttpStatus.CREATED)
        .body(
          new ApiKeyCreatedResponse(clearTextApiKey
                  , ApiKeyDto.fromApiKey(newApiKey)));
    } else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Application with this id does not exist. Could not create ApiKey");
  }

  /**
   * Deletes an api key of an application by its id. The ids of each api key can be seen in the application details
   * @param appId the UUID of the Application, from which to delete the api key from
   * @param apiKeyId the id of the api key to delete
   * @return
   *  204, if the api key got deleted successfully
   *  400, if there are missing/invalid parameters
   */
  @DeleteMapping(path = Delete.DeleteApiKey)
  public ResponseEntity<Void> deleteApiKeyFromAppWithId(
          @PathVariable(Delete.PathParams.applicationId) String appId,
          @PathVariable(Delete.PathParams.apiKeyId) UUID apiKeyId) {

    Optional<Application> optionalApplication = applicationService.findById(UUID.fromString(appId));

    if (optionalApplication.isPresent()) {

      if (!applicationService.isEditableByCurrentUser(optionalApplication.get())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have not created this application or you are not allowed to use it.");
      }

      Application application = optionalApplication.get();
      try{
        applicationService.deleteApiKeyFromApp(application, apiKeyId);
        return ResponseEntity.noContent().build();
      }
      catch(NotFoundException ex){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "ApiKey with this id does not exist. Could not delete ApiKey");
      }

    } else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Application with this id does not exist. Could not delete ApiKey");
  }


  /**
   * Converts the given string into a UUID if it is a valid UUID, if not nothing is returned
   * @param id string which should be converted into a UUID
   * @return Optional with valid UUID or an empty Optional container
   */
  private static Optional<UUID> getUuidFromString(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (Throwable t) {
      log.warn(t.getMessage());
      return Optional.empty();
    }
  }
}

