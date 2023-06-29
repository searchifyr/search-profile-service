package com.github.searchprofileservice.service;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.model.SearchField;
import com.github.searchprofileservice.persistence.mongo.model.SearchProfileDocument;
import com.github.searchprofileservice.persistence.mongo.repository.SearchProfileRepository;
import com.github.searchprofileservice.service.impl.SearchProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SearchProfileServiceTest {

  private final SearchProfileRepository searchProfileRepository
      = mock(SearchProfileRepository.class);
  private final ElasticSearchClientService elasticSearchService
      = mock(ElasticSearchClientService.class);
  private final AuthenticationService authenticationService
      = mock(AuthenticationService.class);

  @InjectMocks
  private final SearchProfileService searchProfileService = new SearchProfileServiceImpl(
      searchProfileRepository, elasticSearchService, authenticationService);

  @Test
  public void getAllSearchProfiles() {
    List<SearchProfileDocument> listOfSearchProfiles = getListOfSearchProfiles();
    when(searchProfileRepository.findAll()).thenReturn(listOfSearchProfiles);

    List<SearchProfileDto> searchProfiles = searchProfileService.getAllSearchProfiles();

    assertThat(searchProfiles.size(), equalTo(listOfSearchProfiles.size()));
  }

  @Test
  public void getAllSearchProfilesByApplicationId() {
    String applicationId = "1313cf79-9d4b-4d11-8a51-ac5417c00b66";
    List<SearchProfileDocument> searchProfileDocuments = getListOfSearchProfiles().stream()
        .filter(
            searchProfileDocument -> searchProfileDocument.getApplicationId().equals(applicationId))
        .toList();
    when(searchProfileRepository.findAllByApplicationId(applicationId))
        .thenReturn(searchProfileDocuments);

    List<SearchProfileDto> searchProfiles = searchProfileService.getAllSearchProfilesByApplicationId(
        applicationId);

    assertThat(searchProfiles.size(), equalTo(searchProfileDocuments.size()));
  }

  @Test
  public void postNewSearchProfile() {
    SearchProfileDto searchProfileDto = getSearchProfileDto();
    SearchProfileDocument searchProfileDocument = SearchProfileDocument.of(searchProfileDto);
    searchProfileDocument.setProfileId(UUID.randomUUID().toString());
    when(searchProfileRepository.save(any())).thenReturn(searchProfileDocument);
    when(authenticationService.getUser())
        .thenReturn(new AuthenticatedUser("testUser", "1", ""));

    SearchProfileDto profileDto = searchProfileService.postNewSearchProfile(searchProfileDto);

    assertNotNull(profileDto);
    assertThat(profileDto.getProfileId(), equalTo(searchProfileDocument.getProfileId()));
    assertThat(profileDto.getApplicationId(), equalTo(searchProfileDto.getApplicationId()));
  }

  @Test
  public void updateSearchProfile() {
    UUID applicationId = UUID.randomUUID();
    String profileId = UUID.randomUUID().toString();
    SearchProfileDto searchProfileDto = getSearchProfileDto();
    searchProfileDto.setProfileId(profileId);
    searchProfileDto.setApplicationId(applicationId);
    searchProfileDto.setName("foo");
    searchProfileDto.setSearchFields(List.of(new SearchField("name", true, 1.0)));
    searchProfileDto.setMinScore(3.14159);

    when(searchProfileRepository.save(any()))
        .thenAnswer(i -> i.getArguments()[0]);
    when(searchProfileRepository.findById(profileId))
        .thenAnswer(i -> {
          var searchProfileDocument =
            getSearchProfileDocument(profileId.toString(), applicationId.toString(), "");
          return Optional.of(searchProfileDocument);
        });
    when(authenticationService.getUser())
        .thenReturn(new AuthenticatedUser("testUser", "1", ""));

    SearchProfileDto resultDto =
        searchProfileService.updateSearchProfile(searchProfileDto, profileId);

    assertNotNull(resultDto);
    assertThat(resultDto.getProfileId(), equalTo(searchProfileDto.getProfileId()));
    assertThat(resultDto.getApplicationId(), equalTo(searchProfileDto.getApplicationId()));
    assertThat(resultDto.getName(), equalTo(searchProfileDto.getName()));
    assertThat(resultDto.getMinScore(), equalTo(searchProfileDto.getMinScore()));
    assertThat(resultDto.getSearchFields(), equalTo(searchProfileDto.getSearchFields()));
  }

  @Test
  public void deleteSearchProfile() {
    String profileId = UUID.randomUUID().toString();
    doNothing().when(searchProfileRepository).deleteById(profileId);

    searchProfileService.deleteSearchProfile(profileId);

    verify(searchProfileRepository, times(1)).deleteById(profileId);
  }

  @Test
  public void existsSearchProfile() {
    String profileId = UUID.randomUUID().toString();
    when(searchProfileRepository.existsById(profileId)).thenReturn(true);

    boolean result = searchProfileService.existsSearchProfile(profileId);

    assertThat(result, equalTo(Boolean.TRUE));
    verify(searchProfileRepository, times(1)).existsById(profileId);
  }


  private SearchProfileDto getSearchProfileDto() {
    String userId = UUID.randomUUID().toString();

    return SearchProfileDto.builder()
        .applicationId(UUID.randomUUID())
        .creatorId(userId)
        .lastEditorId(userId)
        .name("test profile")
        .build();
  }

  private List<SearchProfileDocument> getListOfSearchProfiles() {
    return List.of(
        getSearchProfileDocument("1313cf79-9d4b-4d11-8a51-ac5417c00b66", "test 1"),
        getSearchProfileDocument("1313cf79-9d4b-4d11-8a51-ac5417c00b66", "test 2"),
        getSearchProfileDocument("1313cf79-9d4b-4d11-8a51-ac5417c00b83", "test 3"),
        getSearchProfileDocument("1313cf79-9d4b-4d11-8a51-ac5417c00b23", "test 4")
    );
  }

  private SearchProfileDocument getSearchProfileDocument(String applicationId, String name) {
    return getSearchProfileDocument(UUID.randomUUID().toString(), applicationId, name);
  }

  private SearchProfileDocument getSearchProfileDocument(String profileId, String applicationId, String name) {
    return SearchProfileDocument.builder()
        .profileId(profileId.toString())
        .applicationId(applicationId)
        .creatorId(UUID.randomUUID().toString())
        .lastEditorId(UUID.randomUUID().toString())
        .lastModifiedDate(LocalDateTime.now())
        .name(name)
        .build();
  }

}
