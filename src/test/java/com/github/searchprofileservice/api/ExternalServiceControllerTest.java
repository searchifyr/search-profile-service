package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.ExternalServiceController.DocumentCreateResult;
import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ApplicationService;
import com.github.searchprofileservice.service.SearchProfileService;
import com.github.searchprofileservice.service.SearchService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ExternalServiceControllerTest {
    
  private final SearchService searchService = mock(SearchService.class);
  private final SearchProfileService searchProfileService = mock(SearchProfileService.class);
  private final ApplicationService applicationService = mock(ApplicationService.class);
  @InjectMocks
  private final ExternalServiceController externalServiceController = new ExternalServiceController(searchService, searchProfileService, applicationService);

  @Test
  public void getQueryDefinition_ok() {
    UUID apiKey = UUID.randomUUID(); //is also used as applicationId
    String profileId = UUID.randomUUID().toString();
    String query = "some query";
    SearchProfileDto searchProfileDto = getSearchProfileDtoWithAllParams(apiKey, true);
    Application mockApplication = createTestApplicationHashedApiKey(apiKey);

    //configuration of services
    when(searchProfileService.getSearchProfileByProfileId(any(String.class)))
      .thenReturn(searchProfileDto);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.of(mockApplication));
    when(searchService.getSearchQuery(any(String.class)))
      .thenReturn(query);

    //actual test
    ResponseEntity<String> response = externalServiceController.getQueryDefinition(profileId, apiKey.toString());

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(response.getBody(), equalTo(query));
  }

  @Test
  public void getQueryDefinition_forbidden_blank_apikey() {
    String profileId = UUID.randomUUID().toString();
    String apiKey = "";
    
    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> externalServiceController.getQueryDefinition(profileId, apiKey));

    assertEquals(HttpStatus.FORBIDDEN, responseStatusException.getStatus());
    assertEquals("Api key must not be null or empty in http header field 'Application-Api-Key'.", responseStatusException.getReason());
  }

  @Test
  public void getQueryDefinition_bad_request_uuid_not_valid() {
    String profileId = "profileId";
    String apiKey = UUID.randomUUID().toString();
    
    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> externalServiceController.getQueryDefinition(profileId,  apiKey));

    assertEquals(HttpStatus.BAD_REQUEST, responseStatusException.getStatus());
    assertEquals("'" + profileId + "' is not a valid UUID", responseStatusException.getReason());
  }

  @Test
  public void getQueryDefinition_not_found_searchprofile() {
    UUID apiKey = UUID.randomUUID(); //is also used as applicationId
    String profileId = UUID.randomUUID().toString();

    ResponseStatusException responseStatusException = 
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.getQueryDefinition(profileId,  apiKey.toString()));

    assertEquals(HttpStatus.NOT_FOUND, responseStatusException.getStatus());
    assertEquals("Search profile with id : " + profileId + " does not exist.", responseStatusException.getReason());
  }

  @Test
  public void getQueryDefinition_not_found_application() {
    UUID apiKey = UUID.randomUUID(); //is also used as applicationId
    String profileId = UUID.randomUUID().toString();
    SearchProfileDto searchProfileDto = getSearchProfileDtoWithAllParams(apiKey, true);

    //configuration of services
    when(searchProfileService.getSearchProfileByProfileId(any(String.class)))
      .thenReturn(searchProfileDto);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.empty());

    ResponseStatusException responseStatusException = 
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.getQueryDefinition(profileId, apiKey.toString()));

    assertEquals(HttpStatus.NOT_FOUND, responseStatusException.getStatus());
    assertEquals("Application with id : '" + searchProfileDto.getApplicationId() + "' does not exist.", responseStatusException.getReason());
  }

  @Test
  public void getQueryDefinition_forbidden_apiKeys_do_not_match() {
    UUID apiKey = UUID.randomUUID(); //is also used as applicationId
    String profileId = UUID.randomUUID().toString();
    SearchProfileDto searchProfileDto = getSearchProfileDtoWithAllParams(apiKey, true);
    Application mockApplication = createTestApplicationHashedApiKey(UUID.randomUUID());

    //configuration of services
    when(searchProfileService.getSearchProfileByProfileId(any(String.class)))
      .thenReturn(searchProfileDto);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.of(mockApplication));

    ResponseStatusException responseStatusException = 
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.getQueryDefinition(profileId, apiKey.toString()));

    assertEquals(HttpStatus.FORBIDDEN, responseStatusException.getStatus());
    assertEquals("The given api keys don't match.", responseStatusException.getReason());
  }

  @Test
  public void getQueryDefinition_SearchprofileIsNotQueryable_throwException() {
    UUID apiKey = UUID.randomUUID(); //is also used as applicationId
    String profileId = UUID.randomUUID().toString();
    SearchProfileDto searchProfileDto = getSearchProfileDtoWithAllParams(apiKey, false);
    Application mockApplication = createTestApplicationHashedApiKey(apiKey);

    //configuration of services
    when(searchProfileService.getSearchProfileByProfileId(any(String.class)))
            .thenReturn(searchProfileDto);
    when(applicationService.findById(any(UUID.class)))
            .thenReturn(Optional.of(mockApplication));
    when(searchService.getSearchQuery(any(String.class)))
            .thenThrow(new ResponseStatusException(
            HttpStatus.BAD_REQUEST,"Searchprofile do not support Elastic Querys. Use additional endpoint for Recieving direkt Results"));

    ResponseStatusException responseStatusException =
            assertThrows(ResponseStatusException.class,
                    () -> externalServiceController.getQueryDefinition(profileId,  apiKey.toString()));

    assertEquals(HttpStatus.BAD_REQUEST, responseStatusException.getStatus());
  }

  @Test
  public void uploadDocument_JsonIsMissing_ReturnsFailedServiceResponse() {

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> externalServiceController.uploadDocument(UUID.randomUUID().toString(), null, "test"));
    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);
  }

  @Test
  public void uploadDocument_ApplicationIdIsMissing_ReturnsFailedServiceResponse() {
    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> externalServiceController.uploadDocument(UUID.randomUUID().toString(), "test", null));
    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);

  }

  @Test
  @SneakyThrows
  public void uploadDocument_forbidden_apiKey_blank() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.uploadDocument(null, documentData, applicationId.toString()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.FORBIDDEN);
  }

  @Test
  @SneakyThrows
  public void uploadDocument_not_found_application() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.uploadDocument(applicationId.toString(), documentData, applicationId.toString()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void uploadDocument_forbidden_apiKey_doesnt_match() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.of(createTestApplicationHashedApiKey(UUID.randomUUID())));

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.uploadDocument(applicationId.toString(), documentData, applicationId.toString()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.FORBIDDEN);
  }

  @Test
  @SneakyThrows
  public void uploadDocument_UploadFailed_ReturnsFailedServiceResponse() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID();

    when(applicationService.existsById(applicationId)).thenReturn(true);
    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.of(createTestApplicationHashedApiKey(applicationId)));

    doThrow(IOException.class)
        .when(applicationService).uploadDocument(documentData, applicationId);

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> externalServiceController.uploadDocument(applicationId.toString(), documentData, applicationId.toString()));
    verify(applicationService, times(1))
        .uploadDocument(documentData, applicationId);
    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);

  }

  @Test
  @SneakyThrows
  public void uploadDocument_UploadSuccess_ReturnsSuccessServiceResponse() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication
    var documentId = UUID.randomUUID().toString();

    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.existsById(applicationId)).thenReturn(true);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.of(createTestApplicationHashedApiKey(applicationId)));

    when(applicationService.uploadDocument(documentData, applicationId))
        .then(i -> documentId);

    var response
        = externalServiceController.uploadDocument(applicationId.toString(), documentData, applicationId.toString());
    verify(applicationService, times(1))
        .uploadDocument(documentData, applicationId);
    assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
    assertThat(response.getBody(), equalTo(new DocumentCreateResult(documentId)));
  }

  @Test
  public void updateDocument_JsonIsMissing_ReturnsFailedServiceResponse() {
    var exception = assertThrows(
      ResponseStatusException.class,
      () -> externalServiceController.updateDocument(UUID.randomUUID().toString(), null, UUID.randomUUID().toString(), ""));
    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void updateDocument_ApplicationIdIsMissing_ReturnsFailedServiceResponse() {
    var exception = assertThrows(
      ResponseStatusException.class,
      () -> externalServiceController.updateDocument(UUID.randomUUID().toString(), "test", null, ""));
    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void updateDocument_DocumentIdIsMissing_ReturnsFailedServiceResponse() {
    var exception = assertThrows(
      ResponseStatusException.class,
      () -> externalServiceController.updateDocument(UUID.randomUUID().toString(), "test", UUID.randomUUID().toString(), null));
    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  @SneakyThrows
  public void updateDocument_forbidden_apiKey_blank() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.updateDocument(null, documentData, applicationId.toString(), "id1234"));

    assertEquals(responseStatusException.getStatus(), HttpStatus.FORBIDDEN);
  }

  @Test
  @SneakyThrows
  public void updateDocument_not_found_application() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.updateDocument(applicationId.toString(), documentData, applicationId.toString(), "id1234"));

    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void updateDocument_forbidden_apiKey_doesnt_match() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.of(createTestApplicationHashedApiKey(UUID.randomUUID())));

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.updateDocument(applicationId.toString(), documentData, applicationId.toString(), "id1234"));

    assertEquals(responseStatusException.getStatus(), HttpStatus.FORBIDDEN);
  }

  @Test
  @SneakyThrows
  public void updateDocument_Success() {
    String documentData = "{ \"foo\": \"FOO\" }";
    var applicationId = UUID.randomUUID();
    var documentId = "id1234";

    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.existsById(applicationId)).thenReturn(true);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.of(createTestApplicationHashedApiKey(applicationId)));
    doNothing().when(applicationService).updateDocument(any(String.class), any(UUID.class), any(String.class));

    var response =
      externalServiceController.updateDocument(applicationId.toString(), documentData, applicationId.toString(), documentId);

    verify(applicationService, times(1)).updateDocument(documentData, applicationId, documentId);
    assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
  }

  @Test
  @SneakyThrows
  public void bulkUploadDocuments_forbidden_apiKey_blank() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.bulkUploadDocuments(null, documentData, applicationId.toString()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.FORBIDDEN);
  }

  @Test
  @SneakyThrows
  public void bulkUploadDocuments_bad_request_uuid_not_valid() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = "thisIsAValidUUID"; // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.bulkUploadDocuments(applicationId.toString(), documentData, applicationId));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);
  }

  @Test
  @SneakyThrows
  public void bulkUploadDocuments_not_found_application() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.bulkUploadDocuments(applicationId.toString(), documentData, applicationId.toString()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void bulkUploadDocuments_forbidden_apiKey_doesnt_match() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID(); // also apiKey of mockApplication

    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.findById(any(UUID.class)))
      .thenReturn(Optional.of(createTestApplicationHashedApiKey(UUID.randomUUID())));

    ResponseStatusException responseStatusException =
      assertThrows(ResponseStatusException.class,
        () -> externalServiceController.bulkUploadDocuments(applicationId.toString(), documentData, applicationId.toString()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.FORBIDDEN);
  }

  private SearchProfileDto getSearchProfileDtoWithAllParams(UUID id, boolean queryable) {
    String userId = UUID.randomUUID().toString();

    return SearchProfileDto.builder()
        .profileId(UUID.randomUUID().toString())
        .applicationId(id)
        .analyser(new Analyser())
        .creatorId(userId)
        .lastEditorId(userId)
        .name("test profile")
            .relativeScore(1.0)
            .queryable(queryable)
        .build();
  }

  private Application createTestApplicationHashedApiKey(UUID id){
    ApiKey apiKey = new ApiKey(UUID.randomUUID(), "foo", BCrypt.hashpw(id.toString(), "$2a$14$eWSO45C1macERAjgsSDKLO"));
    List<ApiKey> apiKeys = new ArrayList<>();
    apiKeys.add(apiKey);

    return Application.builder()
			.id(id)
			.createdDate(new Date(System.currentTimeMillis()))
			.applicationName("test")
			.creatorId(UUID.randomUUID().toString())
			.active(true)
      .apiKeys(apiKeys)
			.build();
  }

}
