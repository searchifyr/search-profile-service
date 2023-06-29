package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.ApplicationController.DocumentCreateResult;
import com.github.searchprofileservice.api.model.ApiKeyCreatedResponse;
import com.github.searchprofileservice.api.model.ApplicationCreatedResponse;
import com.github.searchprofileservice.api.model.ApplicationDto;
import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.persistence.mongo.model.User;
import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.*;
import com.github.searchprofileservice.service.impl.ApplicationConverterServiceImpl;
import javassist.NotFoundException;
import lombok.SneakyThrows;
import org.codehaus.plexus.util.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class ApplicationControllerTest {

  private final ApplicationService applicationService = mock(ApplicationService.class);
  private final SearchProfileService searchProfileService = mock(SearchProfileService.class);
  private final AuthenticationService authenticationService = mock(AuthenticationService.class);
  private final ElasticSearchClientService elasticSearchService = mock(ElasticSearchClientService.class);
  private final ApplicationConverterService applicationConverter = new ApplicationConverterServiceImpl(elasticSearchService);

  private final UserService userService = mock(UserService.class);

  @InjectMocks
  private final ApplicationController applicationController
      = new ApplicationController(applicationService, searchProfileService, authenticationService, applicationConverter, userService);

  @Test
  public void getAllApplications_WithAllowedUserFalse_ReturnsAllAppsCurrentUserIsAllowedFor() {
    List<Application> applications =
        List.of(
            new Application(
                UUID.randomUUID(),
                Calendar.getInstance().getTime(),
                new ArrayList<>(),
                "app1",
                "some dude",
                false,
                List.of("111")),
            new Application(
                UUID.randomUUID(),
                Calendar.getInstance().getTime(),
                new ArrayList<>(),
                "app2",
                "another dude",
                false,
                List.of("some dude")),
            new Application(
                UUID.randomUUID(),
                Calendar.getInstance().getTime(),
                new ArrayList<>(),
                "app3",
                "another dude",
                false,
                List.of("some dude"))
        );
    when(applicationService.findAllByAllowedUserIdsContains("some dude"))
            .thenReturn(applications.stream().filter((apps -> apps.getCreatorId().equals("another dude"))).toList());
    when(authenticationService.getUser())
            .thenReturn(new AuthenticatedUser("111", "some dude", ""));

    ResponseEntity<List<ApplicationDto>> response =
            applicationController.getAllApplications(false);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(Objects.requireNonNull(response.getBody()).size(), equalTo(2));
    verify(applicationService).findAllByAllowedUserIdsContains("some dude");
  }

  @Test
  public void getAllApplications_WithAllowedUserTrue_ReturnsAllAppsCreatedByCurrentUser() {
    List<Application> applications =
            List.of(
                    new Application(
                            UUID.randomUUID(),
                            Calendar.getInstance().getTime(),
                            new ArrayList<>(),
                            "app1",
                            "some dude",
                            false,
                            List.of("111")),
                    new Application(
                            UUID.randomUUID(),
                            Calendar.getInstance().getTime(),
                            new ArrayList<>(),
                            "app2",
                            "another dude",
                            false,
                            List.of("some dude")),
                    new Application(
                            UUID.randomUUID(),
                            Calendar.getInstance().getTime(),
                            new ArrayList<>(),
                            "app3",
                            "another dude",
                            false,
                            List.of("some dude"))
            );
    when(applicationService.findAllByUserId("some dude"))
            .thenReturn(applications.stream().filter((apps -> apps.getCreatorId().equals("some dude"))).toList());
    when(authenticationService.getUser())
            .thenReturn(new AuthenticatedUser("111", "some dude", ""));

    ResponseEntity<List<ApplicationDto>> response =
            applicationController.getAllApplications(true);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(Objects.requireNonNull(response.getBody()).size(), equalTo(1));
    verify(applicationService).findAllByUserId("some dude");
  }

  @Test
  public void getApplication() {
    final UUID uuid = UUID.randomUUID();
    long zero = 0;

    Application application =
            Application.builder()
                    .id(uuid)
                    .apiKeys(new ArrayList<ApiKey>())
                    .applicationName("foo")
                    .creatorId("bar")
                    .build();
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(elasticSearchService.getDocumentCountForIndex(any(String.class))).thenReturn(zero);

    ResponseEntity<ApplicationDto> response =
        applicationController.getApplication(uuid.toString());
    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(response.getBody(), equalTo(applicationConverter.convertToApplicationDto(application)));
    verify(applicationService).findById(uuid);
  }

  @Test
  @SneakyThrows
  public void getApplicationWrongUUID() {
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.empty());

    ResponseStatusException responseStatusException =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.getApplication(UUID.randomUUID().toString()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
    verify(applicationService, never()).deleteById(any(UUID.class));
  }

  @Test
  @SneakyThrows
  public void getApplicationBadUUID() {
    assertThrows(
        Exception.class,
        () -> applicationController.getApplication(""));
    verify(applicationService, never()).deleteById(any(UUID.class));
  }

  @Test
  public void createApplication_RequestIsValid_ReturnsSuccessfulResponse() {
    when(authenticationService.getUser())
        .thenReturn(new AuthenticatedUser("testUser", "1", ""));
    when(applicationService.save(any(Application.class)))
            .then(i -> {
              Application a = i.getArgument(0);
              a.setId(UUID.randomUUID());
              a.setApiKeys(new ArrayList<ApiKey>());
              return Optional.of(a);
            });

    ApplicationDto applicationDTO =
        ApplicationDto.builder().name("foo").creatorId("bar").build();
    ResponseEntity<ApplicationCreatedResponse> response =
        applicationController.createApplication(applicationDTO);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
    assertThat(
        Objects.requireNonNull(response.getBody()).getApplicationDto().getName(),
        equalTo(applicationDTO.getName()));
    assertThat(
        response.getBody().getApplicationDto().getCreatorId(),
        equalTo("1"));
    assertThat(response.getBody().getApplicationDto().getId(), is(notNullValue()));
    verify(applicationService).save(any(Application.class));
  }

  @Test
  public void createApplication_NameIsNull_ReturnsFailedServiceResponse() {
    ApplicationDto applicationDTO =
        ApplicationDto.builder().name(null).creatorId("bar").build();

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> applicationController.createApplication(applicationDTO));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);

  }

  @Test
  public void createApplication_CreatorIdIsMissing_ReturnsFailedServiceResponse() {
    when(authenticationService.getUser())
        .thenReturn(new AuthenticatedUser("testUser", "1", ""));
    ApplicationDto applicationDTO =
        ApplicationDto.builder().name("foo").creatorId(null).build();

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> applicationController.createApplication(applicationDTO));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);

  }

  @Test
  public void uploadDocument_JsonIsMissing_ReturnsFailedServiceResponse() {

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> applicationController.uploadDocument(null, "test"));
    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);
  }

  @Test
  public void uploadDocument_ApplicationIdIsMissing_ReturnsFailedServiceResponse() {
    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> applicationController.uploadDocument("test", null));
    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);

  }

  @Test
  @SneakyThrows
  public void uploadDocument_UploadFailed_ReturnsFailedServiceResponse() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID();
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.existsById(applicationId)).thenReturn(true);
    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);
    when(applicationService.findById(applicationId)).thenReturn(Optional.of(application));

    doThrow(IOException.class)
        .when(applicationService).uploadDocument(documentData, applicationId);

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> applicationController.uploadDocument(documentData, applicationId.toString()));
    verify(applicationService, times(1))
        .uploadDocument(documentData, applicationId);
    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);

  }

  @Test
  @SneakyThrows
  public void uploadDocument_UploadSuccess_ReturnsSuccessServiceResponse() {
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID();
    var documentId = UUID.randomUUID().toString();
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.existsById(applicationId)).thenReturn(true);
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);
    when(applicationService.findById(applicationId)).thenReturn(Optional.of(application));

    when(applicationService.uploadDocument(documentData, applicationId))
        .then(i -> documentId);

    var response
        = applicationController.uploadDocument(documentData, applicationId.toString());
    verify(applicationService, times(1))
        .uploadDocument(documentData, applicationId);
    assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
    assertThat(response.getBody(), equalTo(new DocumentCreateResult(documentId)));
  }

  @Test
  void uploadDocument_returns_Unauthorized_on_not_allowed_access() {
    HttpStatus expected = HttpStatus.UNAUTHORIZED;
    String documentData = "{\"Hello\" : \"World\"}";
    var applicationId = UUID.randomUUID();
    Application application =
            Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isJsonValid(documentData)).thenReturn(true);
    when(applicationService.existsById(applicationId)).thenReturn(true);
    when(applicationService.isEditableByCurrentUser(any(Application.class))).thenReturn(false);
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.uploadDocument(documentData, applicationId.toString()));

    assertEquals(response.getStatus(), expected);
  }

  @Test
  public void updateDocument_JsonIsMissing_ReturnsFailedServiceResponse() {
    var exception = assertThrows(
      ResponseStatusException.class,
      () -> applicationController.updateDocument(null, UUID.randomUUID().toString(), ""));
    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void updateDocument_ApplicationIdIsMissing_ReturnsFailedServiceResponse() {
    var exception = assertThrows(
      ResponseStatusException.class,
      () -> applicationController.updateDocument("test", null, ""));
    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void updateDocument_DocumentIdIsMissing_ReturnsFailedServiceResponse() {
    var exception = assertThrows(
      ResponseStatusException.class,
      () -> applicationController.updateDocument("test", UUID.randomUUID().toString(), null));
    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void updateDocument_returns_Unauthorized_on_not_allowed_access() {
    HttpStatus expected = HttpStatus.UNAUTHORIZED;
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isEditableByCurrentUser(any())).thenReturn(false);
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.updateDocument("test", UUID.randomUUID().toString(), UUID.randomUUID().toString()));

    assertEquals(response.getStatus(), expected);
  }

  @Test
  @SneakyThrows
  public void updateDocument_NonExistingApplication_Error() {
    String documentData = "{ \"foo\": \"FOO\" }";
    var applicationId = UUID.randomUUID();
    var documentId = "id1234";
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);
    when(applicationService.findById(applicationId)).thenReturn(Optional.of(application));

    doThrow(IllegalArgumentException.class)
      .when(applicationService)
      .updateDocument(anyString(), any(UUID.class), anyString());

    var exception = assertThrows(
      ResponseStatusException.class,
      () -> applicationController.updateDocument(documentData, applicationId.toString(), documentId));
    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  @SneakyThrows
  public void updateDocument_Success() {
    String documentData = "{ \"foo\": \"FOO\" }";
    var applicationId = UUID.randomUUID();
    var documentId = "id1234";
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.existsById(applicationId)).thenReturn(true);
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);
    when(applicationService.findById(applicationId)).thenReturn(Optional.of(application));
    doNothing().when(applicationService).updateDocument(anyString(), any(UUID.class), anyString());

    var response =
      applicationController.updateDocument(documentData, applicationId.toString(), documentId);

    verify(applicationService, times(1)).updateDocument(documentData, applicationId, documentId);
    assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
  }

  @Test
  public void updateApplication() {
    Application application =
        Application.builder()
            .id(UUID.randomUUID())
            .apiKeys(new ArrayList<ApiKey>())
            .applicationName("foo")
            .creatorId("foo")
            .allowedUserIds(List.of("111"))
            .build();

    when(applicationService.update(any(Application.class)))
        .then(i -> i.getArgument(0));

    when(applicationService.findById(any(UUID.class)))
        .thenReturn(Optional.of(application));

    when(authenticationService.getUser())
            .thenReturn(new AuthenticatedUser("foo", "foo", ""));

    when(userService.getAllUsers())
            .thenReturn(List.of(new User("111", "test-user", "", false)));

    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    ApplicationDto applicationDTO =
        ApplicationDto.builder()
            .name("foo")
            .allowedUserIds(List.of("111"))
            .build();

    ResponseEntity<ApplicationDto> response =
        applicationController.updateApplication(UUID.randomUUID().toString(), applicationDTO);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(Objects.requireNonNull(response.getBody()).getName(),
        equalTo(applicationDTO.getName()));
    assertThat(response.getBody().getId(), equalTo(application.getId()));
    verify(applicationService).update(any(Application.class));
  }

  @Test
  public void updateApplicationBadUUID() {
    ApplicationDto applicationDTO =
        ApplicationDto.builder().name(null).creatorId("bar").build();

    assertThrows(
        Exception.class,
        () -> applicationController.updateApplication("", applicationDTO));
    verify(applicationService, never()).save(any(Application.class));
  }

  @Test
  public void updateApplicationWrongUUID() {
    when(applicationService.findById(any(UUID.class)))
        .thenReturn(Optional.empty());

    ApplicationDto applicationDTO =
        ApplicationDto.builder().name("foo").creatorId("bar").build();

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class, ()
            -> applicationController.updateApplication(UUID.randomUUID().toString(),
            applicationDTO));

    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);

  }

  @Test
  void updateApplication_returns_Unauthorized_on_not_allowed_access() {
    HttpStatus expected = HttpStatus.UNAUTHORIZED;
    ApplicationDto applicationDTO = ApplicationDto.builder().name("111").build();
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isEditableByCurrentUser(any())).thenReturn(false);
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.updateApplication(UUID.randomUUID().toString(), applicationDTO));

    assertEquals(response.getStatus(), expected);
  }

  @Test
  public void updateApplicationPartial1() {
    ApplicationDto applicationDTO =
        ApplicationDto.builder().name(null).creatorId("bar").build();
    assertThrows(
        Exception.class,
        () -> applicationController.updateApplication(null, applicationDTO));
    verify(applicationService, never()).save(any(Application.class));
  }

  @Test
  public void updateApplicationPartial2() {
    ApplicationDto applicationDTO =
        ApplicationDto.builder().name("foo").creatorId(null).build();
    assertThrows(
        Exception.class,
        () -> applicationController.updateApplication(null, applicationDTO));
    verify(applicationService, never()).save(any(Application.class));
  }

  @Test
  void updateApplication_returns_Unauthorized_on_updating_foreign_application() {
    HttpStatus expected = HttpStatus.UNAUTHORIZED;
    ApplicationDto applicationDTO =
            ApplicationDto.builder().name("foo").build();
    Application application =
            Application.builder().applicationName("test").creatorId("123").id(UUID.randomUUID()).build();

    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(authenticationService.getUser()).
            thenReturn(new AuthenticatedUser("test-user", "666", ""));

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.updateApplication(UUID.randomUUID().toString(), applicationDTO));

    assertNotNull(response);
    assertEquals(expected, response.getStatus());
  }

  @Test
  void updateApplication_returns_Bad_Request_on_empty_UserId_in_given_AllowedUserIds () {
    HttpStatus expected = HttpStatus.BAD_REQUEST;
    ApplicationDto applicationDTO =
            ApplicationDto.builder().name("foo").allowedUserIds(List.of(" ")).build();
    Application application =
            Application.builder().applicationName("test").creatorId("123").id(UUID.randomUUID()).build();

    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(authenticationService.getUser()).
            thenReturn(new AuthenticatedUser("test-user", "123", ""));
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.updateApplication(UUID.randomUUID().toString(), applicationDTO));

    assertNotNull(response);
    assertEquals(expected, response.getStatus());
  }

  @Test
  void updateApplication_returns_Bad_Request_on_multiple_identical_UserIds_in_allowed_UserIds() {
    HttpStatus expected = HttpStatus.BAD_REQUEST;
    ApplicationDto applicationDTO =
            ApplicationDto.builder().name("foo").allowedUserIds(List.of("111", "222", "333", "222")).build();
    Application application =
            Application.builder().applicationName("test").creatorId("123").id(UUID.randomUUID()).build();

    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(authenticationService.getUser()).
            thenReturn(new AuthenticatedUser("test-user", "123", ""));
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.updateApplication(UUID.randomUUID().toString(), applicationDTO));

    assertNotNull(response);
    assertEquals(expected, response.getStatus());
  }

  @Test
  void updateApplication_returns_Bad_Request_on_non_existing_User_in_allowed_UserIds() {
    HttpStatus expected = HttpStatus.BAD_REQUEST;
    ApplicationDto applicationDTO =
            ApplicationDto.builder().name("foo").allowedUserIds(List.of("111")).build();
    Application application =
            Application.builder().applicationName("test").creatorId("123").id(UUID.randomUUID()).build();

    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(authenticationService.getUser()).
            thenReturn(new AuthenticatedUser("test-user", "123", ""));
    when(userService.existsByUserId("111")).thenReturn(false);
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.updateApplication(UUID.randomUUID().toString(), applicationDTO));

    assertNotNull(response);
    assertEquals(expected, response.getStatus());
  }

  @Test
  void updateApplication_returns_OK_on_adding_AllowedUserIds() {
    HttpStatus expected = HttpStatus.OK;
    ApplicationDto applicationDTO =
            ApplicationDto.builder().name("foo").allowedUserIds(List.of("111", "222")).build();
    Application application =
            Application.builder().applicationName("test").creatorId("123").id(UUID.randomUUID()).apiKeys(new ArrayList<ApiKey>()).build();

    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(authenticationService.getUser()).
            thenReturn(new AuthenticatedUser("test-user", "123", ""));
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    when(userService.getAllUsers())
            .thenReturn(List.of(
                    new User("111", "test-user1", "", false),
                    new User ("222", "test-user2", "", false)));

    when(applicationService.update(any(Application.class)))
            .then(i -> i.getArgument(0));

    ResponseEntity<ApplicationDto> response =
            applicationController.updateApplication(UUID.randomUUID().toString(), applicationDTO);

    assertNotNull(response);
    assertEquals(expected, response.getStatusCode());
    assertEquals(response.getBody().getAllowedUserIds().get(0), "111");
    assertEquals(response.getBody().getAllowedUserIds().get(1), "222");
  }

  @Test
  @SneakyThrows
  public void deleteApplication() {

    Application application =
            Application.builder()
                    .id(UUID.randomUUID())
                    .apiKeys(new ArrayList<ApiKey>())
                    .applicationName("foo")
                    .creatorId("foo")
                    .allowedUserIds(List.of("111"))
                    .build();

    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(applicationService.existsById(any(UUID.class))).thenReturn(true);
    when(authenticationService.getUser())
            .thenReturn(new AuthenticatedUser("foo", "foo", ""));
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    final UUID uuid = UUID.randomUUID();

    ResponseEntity<Void> response =
        applicationController.deleteApplication(uuid.toString());

    assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
    verify(applicationService).deleteById(uuid);
  }

  @Test
  @SneakyThrows
  public void deleteApplicationWrongUUID() {
    when(applicationService.existsById(any(UUID.class))).thenReturn(false);

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> applicationController.deleteApplication(UUID.randomUUID().toString()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);

    verify(applicationService, never()).deleteById(any(UUID.class));
  }

  @Test
  @SneakyThrows
  public void deleteApplicationBadUUID() {
    assertThrows(
        Exception.class,
        () -> applicationController.deleteApplication(""));
    verify(applicationService, never()).deleteById(any(UUID.class));
  }

  @Test
  void deleteApplication_returns_Unauthorized_on_not_allowed_access() {
    HttpStatus expected = HttpStatus.UNAUTHORIZED;
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isEditableByCurrentUser(any())).thenReturn(false);
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(applicationService.existsById(any(UUID.class))).thenReturn(true);

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.deleteApplication(UUID.randomUUID().toString()));

    assertEquals(response.getStatus(), expected);
  }

  @Test
  public void getAllSearchProfilesByApplicationId() {
    String applicationId = UUID.randomUUID().toString();
    List<SearchProfileDto.BasicProjection> listOfSearchProfiles =
      List.of(
        new SearchProfileDto.BasicProjection("", "", "", "", LocalDateTime.now(), "sp1"),
        new SearchProfileDto.BasicProjection("", "", "", "", LocalDateTime.now(), "sp2")
      );
    Application application = new Application(
            UUID.randomUUID(),
            Calendar.getInstance().getTime(),
            new ArrayList<>(),
            "app1",
            "some dude",
            false,
            List.of("111"));

    when(searchProfileService.getAllSearchProfilesByApplicationIdAsBasicProjection(applicationId))
      .thenReturn(listOfSearchProfiles);
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));

    ResponseEntity<List<SearchProfileDto.BasicProjection>> profiles =
        applicationController.getAllSearchProfilesBy(applicationId);

    assertThat(profiles.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(Objects.requireNonNull(profiles.getBody()).size(), equalTo(2));
  }

  @Test
  public void createNewApiKeyForAppWithId_happyPath(){
    Application mockApplication = Application.builder().id(UUID.randomUUID()).build();
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(mockApplication));
    doNothing().when(applicationService).addNewApiKeyToApp(any(Application.class), any(ApiKey.class));
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    ResponseEntity<ApiKeyCreatedResponse> response = applicationController
            .createNewApiKeyForAppWithId(UUID.randomUUID().toString(), "newApiKey");

    assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
    assertThat(response.getBody().getApiKey().getName(), equalTo("newApiKey"));
    assertTrue(StringUtils.isNotBlank(response.getBody().getClearTextApiKey()));
  }
  @Test
  public void createNewApiKeyForAppWithId_nameIsEmpty_BadRequest(){
    ResponseStatusException response = assertThrows(ResponseStatusException.class, () -> applicationController
            .createNewApiKeyForAppWithId(UUID.randomUUID().toString(), null));

    assertThat(response.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }
  @Test
  public void createNewApiKeyForAppWithId_AppDoesNotExist_BadRequest(){
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.empty());

    ResponseStatusException response = assertThrows(ResponseStatusException.class, () -> applicationController
            .createNewApiKeyForAppWithId(UUID.randomUUID().toString(), "newApiKey"));

    assertThat(response.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void createNewApiKey_returns_Unauthorized_on_not_allowed_access() {
    HttpStatus expected = HttpStatus.UNAUTHORIZED;
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isEditableByCurrentUser(any())).thenReturn(false);
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.createNewApiKeyForAppWithId(UUID.randomUUID().toString(), "test"));

    assertEquals(response.getStatus(), expected);

  }

  @SneakyThrows
  @Test
  public void deleteApiKeyFromAppWithId_happyPath(){
    Application mockApplication = Application.builder().id(UUID.randomUUID()).build();
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(mockApplication));
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    ResponseEntity<Void> response = applicationController
            .deleteApiKeyFromAppWithId(UUID.randomUUID().toString(), UUID.randomUUID());

    assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
  }

  @SneakyThrows
  @Test
  public void deleteApiKeyFromAppWithId_apiKeyDoesNotExist_BadRequest(){
    Application mockApplication = Application.builder().id(UUID.randomUUID()).build();
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(mockApplication));
    when(applicationService.isEditableByCurrentUser(any())).thenReturn(true);

    doThrow(new NotFoundException("")).when(applicationService).deleteApiKeyFromApp(any(Application.class), any(UUID.class));

    ResponseStatusException response = assertThrows(ResponseStatusException.class, () -> applicationController
            .deleteApiKeyFromAppWithId(UUID.randomUUID().toString(), UUID.randomUUID()));

    assertThat(response.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void deleteApiKeyFromAppWithId_ApplicationDoesNotExist_BadRequest(){
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.empty());

    ResponseStatusException response = assertThrows(ResponseStatusException.class, () -> applicationController
            .deleteApiKeyFromAppWithId(UUID.randomUUID().toString(), UUID.randomUUID()));

    assertThat(response.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void deleteApiKeyFromAppWithId_returns_Unauthorized_on_not_allowed_access() {
    HttpStatus expected = HttpStatus.UNAUTHORIZED;
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isEditableByCurrentUser(any())).thenReturn(false);
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.deleteApiKeyFromAppWithId(UUID.randomUUID().toString(), UUID.randomUUID()));

    assertEquals(response.getStatus(), expected);
  }

  @Test
  void bulkUploadDocuments_returns_Unauthorized_on_not_allowed_access() {
    HttpStatus expected = HttpStatus.UNAUTHORIZED;
    String json = "test";
    UUID applicationId = UUID.randomUUID();
    Application application = Application.builder().id(UUID.randomUUID()).build();

    when(applicationService.isEditableByCurrentUser(any())).thenReturn(false);
    when(applicationService.findById(any(UUID.class))).thenReturn(Optional.of(application));
    when(applicationService.existsById(applicationId)).thenReturn(true);
    when(applicationService.isJsonValid(json)).thenReturn(true);

    ResponseStatusException response =
            assertThrows(ResponseStatusException.class,
                    () -> applicationController.bulkUploadDocuments(json, UUID.randomUUID().toString()));

    assertEquals(response.getStatus(), expected);
  }

}
