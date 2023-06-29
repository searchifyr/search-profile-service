package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.model.SearchField;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ApplicationService;
import com.github.searchprofileservice.service.SearchProfileService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SearchProfileControllerTest {

  private final SearchProfileService searchProfileService = mock(SearchProfileService.class);
  private final ApplicationService applicationService = mock(ApplicationService.class);


  @InjectMocks
  private final SearchProfileController searchProfileController
      = new SearchProfileController(searchProfileService, applicationService);


  @Test
  public void getAllSearchProfiles() {
    List<SearchProfileDto.BasicProjection> listOfSearchProfiles =
      List.of(
        new SearchProfileDto.BasicProjection("", "", "", "", LocalDateTime.now(), "sp1"),
        new SearchProfileDto.BasicProjection("", "", "", "", LocalDateTime.now(), "sp2")
      );
    when(searchProfileService.getAllSearchProfilesAsBasicProjection()).thenReturn(listOfSearchProfiles);

    ResponseEntity<List<SearchProfileDto.BasicProjection>> profiles =
        searchProfileController.getAllSearchProfiles();

    assertThat(profiles.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(Objects.requireNonNull(profiles.getBody()).size(), equalTo(2));
  }

  @Test
  public void getSearchProfileById_findsProfileByID() {
    String profileId = "91e0380c-1d4e-4547-8384-75de7c82a954";
    SearchProfileDto searchProfileDto = getSearchProfileDtoWithAllParams();

    when(searchProfileService.getSearchProfileByProfileId(eq(profileId)))
      .thenReturn(searchProfileDto);
    when(searchProfileService.loadSearchFieldKeysForSearchProfile(eq(searchProfileDto)))
      .thenReturn(
        Optional.ofNullable(searchProfileDto.getSearchFields()).orElseGet(Collections::emptyList)
          .stream()
          .map(field -> field.getFieldName())
          .collect(Collectors.toSet())
      );

    ResponseEntity<SearchProfileDto> response =
      searchProfileController.getSearchProfileById(profileId);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertNotNull(response.getBody());
    assertThat(searchProfileDto, equalTo(response.getBody()));
  }

  @Test
  public void getSearchProfileById_IdIsNotValidUUID() {
    String profileId = "testProfileID";    

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> searchProfileController.getSearchProfileById(profileId));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);
  }
  
  @Test
  public void getSearchProfileById_returnsFieldsFromElasticSearch() {
    final List<SearchField> initialSearchFields = 
      List.of(
        new SearchField("foo", true, 3.0),
        new SearchField("bar", true, 2.0));

    final List<SearchField> currentlyAvailableSearchFields =
      List.of(
        new SearchField("bar", true, 2.0),
        new SearchField("baz", false, 1.0));

    SearchProfileDto searchProfileDto = getSearchProfileDtoWithAllParams();
    searchProfileDto.setSearchFields(new ArrayList<>(initialSearchFields));
        
    when(
      searchProfileService.loadSearchFieldKeysForSearchProfile(any(SearchProfileDto.class))
    ).thenReturn(
        currentlyAvailableSearchFields
          .stream()
          .map(field -> field.getFieldName())
          .collect(Collectors.toSet())
    );

    when(searchProfileService.getSearchProfileByProfileId(any(String.class)))
      .thenReturn(searchProfileDto);

    ResponseEntity<SearchProfileDto> response =
      searchProfileController.getSearchProfileById(UUID.randomUUID().toString());

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getSearchFields());
    assertThat(
      response.getBody().getSearchFields().size(),
      equalTo(currentlyAvailableSearchFields.size()));

    for (var field : currentlyAvailableSearchFields) {
      assertTrue(response.getBody().getSearchFields().contains(field));
    }
  }

  @Test
  public void createSearchProfile_happyPath_works() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    profileDto.setSearchFields(Collections.emptyList());

    final Optional<Application> mockApplication =
      Optional.of(
        new Application(
          UUID.randomUUID(),
          new Date(),
          "application name",
          true,
          List.of("111")));

    when(applicationService.findById(any(UUID.class))).thenReturn(mockApplication);
    when(applicationService.isEditableByCurrentUser(any(Application.class)))
            .thenReturn(true);

    when(searchProfileService.postNewSearchProfile(any(SearchProfileDto.class)))
      .thenReturn(profileDto);

    ResponseEntity<SearchProfileDto> response = 
      searchProfileController.postNewSearchProfile(profileDto);

    assertEquals(response.getStatusCode(), HttpStatus.CREATED);
  }

  @Test
  public void createSearchProfile_forPrivateAppByUnauthorizedUser_ThrowsException() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    profileDto.setSearchFields(Collections.emptyList());

    final Optional<Application> mockApplication =
            Optional.of(
                    new Application(
                            UUID.randomUUID(),
                            new Date(),
                            "application name",
                            true,
                            List.of("111")
                    ));
    when(applicationService.findById(any(UUID.class))).thenReturn(mockApplication);
    when(applicationService.isEditableByCurrentUser(any(Application.class))).thenReturn(false);
    when(searchProfileService.postNewSearchProfile(any(SearchProfileDto.class)))
            .thenReturn(profileDto);

    ResponseStatusException responseStatusException =
            assertThrows(ResponseStatusException.class,
                    () -> searchProfileController.postNewSearchProfile(profileDto));

    assertEquals(responseStatusException.getStatus(), HttpStatus.UNAUTHORIZED);
  }

  @Test
  public void createSearchProfile_BadRequest_Negative_Field_Boost() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    profileDto.setProfileId(UUID.randomUUID().toString());
    profileDto.setSearchFields(List.of(new SearchField("field_name", true, -1d)));

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> searchProfileController.postNewSearchProfile(profileDto));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);
    assertEquals(responseStatusException.getReason(), "'Field boost must not be negative'");
  }

  @Test
  public void updateSearchProfile_happyPath_works(){
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    profileDto.setProfileId(UUID.randomUUID().toString());

    final Optional<Application> mockApplication =
            Optional.of(
                    new Application(
                            UUID.randomUUID(),
                            new Date(),
                            "application name",
                            true,
                            List.of("111")
                    ));
    when(applicationService.findById(any(UUID.class))).thenReturn(mockApplication);
    when(applicationService.isEditableByCurrentUser(any(Application.class)))
            .thenReturn(true);
    when(searchProfileService.updateSearchProfile(any(SearchProfileDto.class), any(String.class)))
            .thenReturn(profileDto);

    ResponseEntity<SearchProfileDto> response = searchProfileController.updateSearchProfile(profileDto, profileDto.getProfileId());
    SearchProfileDto responseBody = response.getBody();

    assert responseBody != null;
    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(responseBody.getProfileId(), equalTo(profileDto.getProfileId()));
    assertThat(responseBody.getApplicationId(), equalTo(profileDto.getApplicationId()));
  }

  @Test
  public void updateSearchProfile_forPrivateAppByUnauthorizedUser_ThrowsException() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    profileDto.setProfileId(UUID.randomUUID().toString());

    final Optional<Application> mockApplication =
            Optional.of(
                    new Application(
                            UUID.randomUUID(),
                            new Date(),
                            "application name",
                            true,
                            List.of("111")
                    ));
    when(applicationService.findById(any(UUID.class))).thenReturn(mockApplication);
    when(applicationService.isEditableByCurrentUser(any(Application.class))).thenReturn(false);
    when(searchProfileService.updateSearchProfile(any(SearchProfileDto.class), any(String.class)))
            .thenReturn(profileDto);

    ResponseStatusException responseStatusException =
            assertThrows(ResponseStatusException.class,
                    () -> searchProfileController.updateSearchProfile(profileDto, profileDto.getProfileId()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.UNAUTHORIZED);
  }

  @Test
  public void updateSearchProfile_BadRequest() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    profileDto.setProfileId(UUID.randomUUID().toString());
    profileDto.setApplicationId(null);

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> searchProfileController.updateSearchProfile(profileDto,
                profileDto.getProfileId()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);

  }

  @Test
  public void updateSearchProfile_BadRequest_UUID_not_valid() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    String profileId = "profileId";

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> searchProfileController.updateSearchProfile(profileDto,
                profileId));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);
    assertEquals(responseStatusException.getReason(), "'" + profileId + "' is not a valid UUID");
  }

  @Test
  public void updateSearchProfile_BadRequest_Negative_Field_Boost() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    profileDto.setProfileId(UUID.randomUUID().toString());
    profileDto.setSearchFields(List.of(new SearchField("field_name", true, -1d)));

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> searchProfileController.updateSearchProfile(
                    profileDto,
                    profileDto.getProfileId()));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);
    assertEquals(responseStatusException.getReason(), "'Field boost must not be negative'");
  }

  @Test
  public void updateSearchProfile_not_found_profile() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    profileDto.setProfileId(UUID.randomUUID().toString());
    String wrongProfileId = UUID.randomUUID().toString();

    final Optional<Application> mockApplication =
            Optional.of(
                    new Application(
                            UUID.randomUUID(),
                            new Date(),
                            "application name",
                            true,
                            List.of("111")
                    ));
    when(applicationService.findById(any(UUID.class))).thenReturn(mockApplication);
    when(applicationService.isEditableByCurrentUser(any(Application.class)))
            .thenReturn(true);

    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
    "No search profile exists for the given profileId"))
      .when(searchProfileService).updateSearchProfile(profileDto, wrongProfileId);

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> searchProfileController.updateSearchProfile(profileDto,
            wrongProfileId));
            
    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
    assertEquals(responseStatusException.getReason(), "No search profile exists for the given profileId");
  }

  @Test
  public void updateSearchProfile_not_found_application() {
    SearchProfileDto profileDto = getSearchProfileDtoWithAllParams();
    String profileId = UUID.randomUUID().toString();

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> searchProfileController.updateSearchProfile(profileDto,
                profileId));

    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
    assertEquals(responseStatusException.getReason(), "No application with id: " + profileDto.getApplicationId() + " found.");
  }

  private ResponseEntity<SearchProfileDto> updateSearchProfile(SearchProfileDto profileDto,
      boolean exists) {
    when(searchProfileService.existsSearchProfile(profileDto.getProfileId())).thenReturn(exists);
    when(searchProfileService.updateSearchProfile(any(), any())).thenReturn(profileDto);

    return searchProfileController.updateSearchProfile(profileDto, profileDto.getProfileId());
  }

  @SneakyThrows
  @Test
  public void deleteSearchProfile_NotFound() {
    String profileId = UUID.randomUUID().toString();

    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class, ()
            -> searchProfileController.deleteSearchProfile(profileId));

    assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);

  }

  @SneakyThrows
  @Test
  public void deleteSearchProfile() {
    String profileId = UUID.randomUUID().toString();
    SearchProfileDto searchProfileDto = getSearchProfileDtoWithAllParams();

    final Optional<Application> mockApplication =
            Optional.of(
                    new Application(
                            UUID.randomUUID(),
                            new Date(),
                            "application name",
                            true,
                            List.of("111")
                    ));

    when(searchProfileService.existsSearchProfile(eq(profileId))).thenReturn(true);
    when(searchProfileService.getSearchProfileByProfileId(profileId)).thenReturn(searchProfileDto);
    when(applicationService.findById(any(UUID.class))).thenReturn(mockApplication);
    when(applicationService.isEditableByCurrentUser(any(Application.class))).thenReturn(true);

    ResponseEntity<Void> response = searchProfileController.deleteSearchProfile(
        profileId);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
    verify(searchProfileService).deleteSearchProfile(profileId);
  }

  @SneakyThrows
  @Test
  public void deleteSearchProfile_BadRequest() {
    ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class, ()
            -> searchProfileController.deleteSearchProfile(""));

    assertEquals(responseStatusException.getStatus(), HttpStatus.BAD_REQUEST);
  }

  private SearchProfileDto copyProfileAndAddProfileId(SearchProfileDto profileDto) {
    return SearchProfileDto.builder()
        .profileId(UUID.randomUUID().toString())
        .applicationId(profileDto.getApplicationId())
        .creatorId(profileDto.getCreatorId())
        .lastEditorId(profileDto.getLastEditorId())
        .name(profileDto.getName())
        .build();
  }

  private SearchProfileDto getSearchProfileDtoWithAllParams() {
    String userId = UUID.randomUUID().toString();

    return SearchProfileDto.builder()
        .applicationId(UUID.randomUUID())
        .analyser(new Analyser())
        .creatorId(userId)
        .lastEditorId(userId)
        .name("test profile")
        .build();
  }

  private List<SearchProfileDto> getListOfSearchProfiles() {
    return List.of(
        new SearchProfileDto(),
        new SearchProfileDto()
    );
  }
}
