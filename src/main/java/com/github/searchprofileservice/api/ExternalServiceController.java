package com.github.searchprofileservice.api;

import com.github.searchprofileservice.model.SearchResults;
import com.github.searchprofileservice.api.routes.Routes.Api.V1.externalServices.Applications.GetOne;
import com.github.searchprofileservice.api.routes.Routes.Api.V1.externalServices.query;
import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ApplicationService;
import com.github.searchprofileservice.service.SearchProfileService;
import com.github.searchprofileservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.compress.NotXContentException;
import org.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 
 * Controller used by persons who want to use backend services without being authenticated via OAuth but give f.e. a valid api key of an application
 * 
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ExternalServiceController {

  private final SearchService searchService;
  private final SearchProfileService searchProfileService;
  private final ApplicationService applicationService;

  /**
   * Describes the result of inserting a new document into elastic search.
   * Encapsulates the document's id.
   */
  public static record DocumentCreateResult(String documentId) { }

  /**
   * Returns query definition for given search-profile using api key to authenticate developers of external services
   * @param apiKey apiKey of an application, must be set in a custom http header named 'Application-Api-Key'
   * @param profileId profileId of search profile the query definition should be returned from
   * 
   * @return
   * 200, if returning query definition of given search-profile was successful<br /><br />
   * 400, if given search-profileId is not a valid uuid or if searchValue is blank<br /><br />
   * 403, if given api key is blank or does not match api key of application the given search-profile belongs to<br /><br />
   * 404, if no search-profile or application could be found with given search-profileId (second should not occur because application a profile belongs to should always exist)
   */
  @GetMapping(path = query.get)
  public ResponseEntity<String> getQueryDefinition(@RequestParam("profileId") String profileId, @RequestHeader("Application-Api-Key") String apiKey) {
    
    if (StringUtils.isBlank(apiKey)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Api key must not be null or empty in http header field 'Application-Api-Key'.");
    }

    getUuidFromString(profileId).orElseThrow(() -> new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "'" + profileId + "' is not a valid UUID"));

    UUID applicationId;
    try {
      applicationId = searchProfileService.getSearchProfileByProfileId(profileId).getApplicationId();
    } catch(Exception e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Search profile with id : " + profileId + " does not exist.");
    }

    Application application = applicationService.findById(applicationId).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application with id : '" + applicationId + "' does not exist."));

    checkApiKeyAndThrowExceptionOnNoMatch(apiKey, application.getApiKeys());

      String query = searchService.getSearchQuery(profileId);
      return ResponseEntity.ok(query);
  }

  /**
   * Uploads a JSON document into the elastic search index of an application
   * @param apiKey apiKey of an application, must be set in a custom http header named 'Application-Api-Key'
   * @param json the document to upload
   * @param applicationId the id of the application into whose index to insert the document
   * 
   * @return
   * 201, if upload was successful<br /><br />
   * 400, if given applicationId is not a valid uuid or if given json is blank or not valid<br /><br />
   * 403, if given api key is blank or does not match api key of given application<br /><br />
   * 404, if no application could be found with given applicationId
   */
  @PostMapping(path = GetOne.PostDocument)
  public ResponseEntity<DocumentCreateResult> uploadDocument(
          @RequestHeader("Application-Api-Key") String apiKey,
          @RequestBody String json,
          @PathVariable(GetOne.PathParams.applicationId) String applicationId) {
    
    if (StringUtils.isBlank(json)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' must not be null or empty.");
    } else if(!applicationService.isJsonValid(json)){
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' is not satisfying Json standard");
    } else if (StringUtils.isBlank(apiKey)) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, 
        "Api key must not be null or empty in http header field 'Application-Api-Key'.");
    }

    UUID id = getUuidFromString(applicationId).orElseThrow(() -> new ResponseStatusException(
      HttpStatus.BAD_REQUEST, "'" + applicationId + "' is not a valid UUID"));

    Application application = applicationService.findById(id).orElseThrow(() -> new ResponseStatusException(
      HttpStatus.NOT_FOUND, "Application with id : '" + id + "' does not exist."));

    checkApiKeyAndThrowExceptionOnNoMatch(apiKey, application.getApiKeys());

    try {
      String documentId = applicationService.uploadDocument(json, id);
      return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(new DocumentCreateResult(documentId));
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indexing Json Failed.");
    }
  }

  /**
   * Updates a JSON document inside elastic search or creates a new one
   * @param apiKey apiKey of an application, must be set in a custom http header named 'Application-Api-Key'
   * @param json the document to upload
   * @param applicationId the id of the application into whose index the document should be updated
   * @param documentId the id of the document to update
   * 
   * @return
   * 201, if upload was successful<br /><br />
   * 400, if given applicationId is not a valid uuid or if given json is blank or not valid<br /><br />
   * 403, if given api key is blank or does not match api key of given application<br /><br />
   * 404, if no application could be found with given applicationId
   */
  @PutMapping(path = GetOne.PutDocument.route)
  public ResponseEntity<Void> updateDocument(
          @RequestHeader("Application-Api-Key") String apiKey,
          @RequestBody String json,
          @PathVariable(GetOne.PathParams.applicationId) String applicationId,
          @PathVariable(GetOne.PutDocument.PathParams.documentId) String documentId) {
    
    if (StringUtils.isBlank(json)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' must not be null or empty.");
    } else if(!applicationService.isJsonValid(json)){
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' is not satisfying Json standard");
    } else if (StringUtils.isBlank(apiKey)) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, 
        "Api key must not be null or empty in http header field 'Application-Api-Key'.");
    } else if (StringUtils.isBlank(documentId)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'DocumentId' must not be null or empty");
    }

    UUID id = getUuidFromString(applicationId).orElseThrow(() -> new ResponseStatusException(
      HttpStatus.BAD_REQUEST, "'" + applicationId + "' is not a valid UUID"));

    Application application = applicationService.findById(id).orElseThrow(() -> new ResponseStatusException(
      HttpStatus.NOT_FOUND, "Application with id : '" + id + "' does not exist."));
    
    checkApiKeyAndThrowExceptionOnNoMatch(apiKey, application.getApiKeys());

    try {
      applicationService.updateDocument(json, id, documentId);
      return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indexing Json Failed.");
    }
  }

  /**
   * Upload multiple Json Documents to an existing ElasticSearch Application/Index
   * @param apiKey apiKey of an application, must be set in a custom http header named 'Application-Api-Key'
   * @param bulkJson Json File which contains multiple Json Documents for uploading to an ElasticSearch Application
   * @param applicationId the id of the application into whose index to insert the document
   * 
   * @return
   * 201, if upload was successful<br /><br />
   * 400, if given applicationId is not a valid uuid or if given json is blank or not valid<br /><br />
   * 403, if given api key is blank or does not match api key of given application<br /><br />
   * 404, if no application could be found with given applicationId
   */
  @PostMapping(path = GetOne.PostBulkUpload)
  public ResponseEntity<List<String>> bulkUploadDocuments(
          @RequestHeader ("Application-Api-Key") String apiKey,
          @RequestBody String bulkJson,
          @PathVariable (GetOne.PathParams.applicationId) String applicationId) {
    
    if (StringUtils.isBlank(bulkJson)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' must not be null or empty.");
    } else if(!applicationService.isJsonValid(bulkJson)){
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "'Json Document' is not satisfying Json standard");
    } else if (StringUtils.isBlank(apiKey)) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, 
        "Api key must not be null or empty in http header field 'Application-Api-Key'.");
    }

    UUID id = getUuidFromString(applicationId).orElseThrow(() -> new ResponseStatusException(
      HttpStatus.BAD_REQUEST, "'" + applicationId + "' is not a valid UUID"));

    Application application = applicationService.findById(id).orElseThrow(() -> new ResponseStatusException(
      HttpStatus.NOT_FOUND, "Application with id : '" + id + "' does not exist."));
    
    checkApiKeyAndThrowExceptionOnNoMatch(apiKey, application.getApiKeys());

    try {
      List<String> successfulDocIds = applicationService.bulkUploadDocuments(bulkJson, id);
      return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(successfulDocIds);
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indexing Json Failed.");
    } catch (NotXContentException e){
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indexing Json Failed.");
    } catch (JSONException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documents are not satisfying Json standard");
    }
  }

  /**
   * Executes a search query from a search profile with elastic search using a apiKey to authenticate
   *
   * @return
   *  200, if search was successful
   *  400, if the given search-profileId is not a valid UUID or searchValue is blank
   *  403, if the given api key is blank or does not match api key of application the given search-profile belongs to
   *  404, if no search-profile or application could be found with given search-profile id
   */
  @GetMapping(path = query.getQueryResult)
  public ResponseEntity<SearchResults> getQueryResult(
          @RequestParam("profileId") String profileId,
          @RequestParam("searchValue") String searchValue,
          @RequestHeader("Application-Api-Key") String apiKey) {

    if (StringUtils.isBlank(apiKey)) {
      throw new ResponseStatusException(
              HttpStatus.FORBIDDEN, "Api key must not be null or empty in http header field 'Application-Api-Key'.");
    }
    if (StringUtils.isBlank(searchValue)) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Search value must not be null or empty.");
    }

    getUuidFromString(profileId).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "'" + profileId + "' is not a valid UUID"));

    UUID applicationId;
    try {
      applicationId = searchProfileService.getSearchProfileByProfileId(profileId).getApplicationId();
    } catch(Exception e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Search profile with id : " + profileId + " does not exist.");
    }

    Application application = applicationService.findById(applicationId).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application with id : '" + applicationId + "' does not exist."));

    checkApiKeyAndThrowExceptionOnNoMatch(apiKey, application.getApiKeys());

    var results = searchService.search(profileId,searchValue);

    return ResponseEntity.ok(results);
  }

   /**
   * Converts the given string into a UUID if it's a valid UUID, if not nothing is returned
   * @param id string which should be converted into a UUID
   * @return Optional with valid UUID or an empty Optional container
   */
  private static Optional<UUID> getUuidFromString(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (Throwable t) {
      return Optional.empty();
    }
  }

  /**
   * Check that an unhashed api key matches a hashed one and if not throws an Exception
   * @param unhashedApiKey unhashed api key
   * @param apiKeys all api key objects of an application
   * @throws ResponseStatusException with http status {@code FORBIDDEN} if api keys don't match
   */
  private static void checkApiKeyAndThrowExceptionOnNoMatch(String unhashedApiKey, List<ApiKey> apiKeys) throws ResponseStatusException {
    List<String> hashedKeys = apiKeys.stream().map(ApiKey::getKey).toList();

    if(hashedKeys.stream().noneMatch(hashedKey -> BCrypt.checkpw(unhashedApiKey, hashedKey))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The given api keys don't match.");
    }
  }
}
