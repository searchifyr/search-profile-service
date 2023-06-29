package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.model.SearchResults;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ApplicationService;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.SearchService;
import lombok.Builder;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchControllerTest {

  private final SearchService searchService = mock(SearchService.class);
  private final AuthenticationService authenticationService = mock(AuthenticationService.class);
  private final ApplicationService applicationService = mock(ApplicationService.class);
  @InjectMocks
  private final SearchController searchController = new SearchController(searchService, authenticationService, applicationService);

  @Test
  public void getSearchResults_ok() {
    String profileId = UUID.randomUUID().toString();
    String searchValue = "searchText";
    when(searchService.search(profileId, searchValue)).thenReturn(new SearchResults());

    ResponseEntity<SearchResults> response = searchController.getSearchResults(profileId,
        searchValue);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
  }

  @Test
  public void getSearchResults_badRequest() {
    String profileId = UUID.randomUUID().toString();
    String searchValue = "";

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> searchController.getSearchResults(profileId, searchValue));

    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void getSearchResults_bad_Request_UUID_not_valid() {
    String profileId = "profileId";
    String searchValue = "name";

    ResponseStatusException responseStatusException = assertThrows(
        ResponseStatusException.class,
        () -> searchController.getSearchResults(profileId, searchValue));

        assertThat(responseStatusException.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(responseStatusException.getReason(), equalTo("'" + profileId + "' is not a valid UUID"));
  }

  @Test
  public void getQuery_ok() {
    String profileId = UUID.randomUUID().toString();
    when(searchService.getSearchQuery(profileId)).thenReturn("queryAsString");

    ResponseEntity<String> response = searchController.getQuery(profileId);
    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(response.getBody(), equalTo("queryAsString"));
  }

  @Test
  public void getQuery_badRequest() {
    String profileId = "noUUID";

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> searchController.getQuery(profileId));

    assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void getQuery_bad_request_UUID_not_valid() {
    String profileId = "ThisIsAProfileId";

    ResponseStatusException responseStatusException = assertThrows(
        ResponseStatusException.class,
        () -> searchController.getQuery(profileId));

    assertThat(responseStatusException.getStatus(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(responseStatusException.getReason(), equalTo("'" + profileId + "' is not a valid UUID"));
  }

  @Builder
  @Test
  public void postTestSearchProfile_ok(){
    final UUID uuid = UUID.randomUUID();
    final String mockCreatorId = "mockUserId";

    String searchValue = "name";

    SearchProfileDto searchProfileDto = SearchProfileDto.builder()
            .applicationId(uuid)
            .creatorId(mockCreatorId)
            .lastEditorId("123")
            .name("test profile")
            .build();

    //Config
    Application application = createTestApplication(uuid,mockCreatorId);
    when(applicationService.findById(uuid)).thenReturn(Optional.of(application));
    when(authenticationService.getUser()).thenReturn(new AuthenticatedUser("mockUser",mockCreatorId, ""));
    when(searchService.searchByProfileDTO(searchProfileDto, searchValue)).thenReturn(new SearchResults());

    //Assert
    ResponseEntity<SearchResults> response1 = searchController.postTestSearchProfile(searchProfileDto, searchValue);
    assertThat(response1.getStatusCode(), equalTo(HttpStatus.OK));
  }

  private Application createTestApplication(UUID uuid, String creatorId){
    return Application.builder()
			.id(uuid)
			.createdDate(new Date(System.currentTimeMillis()))
			.apiKeys(new ArrayList<>())
			.applicationName("test")
			.creatorId(creatorId)
			.active(false)
			.build();
  }
}
