package com.github.searchprofileservice.service;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.client.ElasticSearchStatefulClient;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchServiceTest {

  SearchProfileService searchProfileService = mock(SearchProfileService.class);
  ElasticSearchStatefulClient elasticSearchStatefulClient = mock(ElasticSearchStatefulClient.class);

  @InjectMocks
  private final SearchService searchService = new SearchServiceImpl(searchProfileService,
      elasticSearchStatefulClient);

  @Test
  public void search() throws IOException {
    String profileId = UUID.randomUUID().toString();
    String searchValue = "searchText";
    mockCreateSearchRequest(profileId, true);
    when(elasticSearchStatefulClient.search(any())).thenThrow(new IOException());

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> searchService.search(profileId, searchValue));

    assertThat(exception.getStatus(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void getSearchQueryOptional() {
    String placeholderSearchValue = "{{placeholder}}";
    String profileId = UUID.randomUUID().toString();
    mockCreateSearchRequest(profileId, true);
    String searchQuery = searchService.getSearchQuery(profileId);

    assertThat(searchQuery, containsString(placeholderSearchValue));
  }

  @Test
  public void getSearchQuery() {
    String profileId = UUID.randomUUID().toString();
    String searchValue = "searchText";
    mockCreateSearchRequest(profileId, true);
    String searchQuery = searchService.getSearchQuery(profileId, searchValue);

    assertThat(searchQuery, containsString(searchValue));
  }

  @Test
  public void getSearchQuery_SearchprofileIsNotQueryable_throwsException() {
    String profileId = UUID.randomUUID().toString();
    String searchValue = "searchText";
    mockCreateSearchRequest(profileId, false);
    assertThrows(
            ResponseStatusException.class,
            () -> searchService.getSearchQuery(profileId, searchValue)
    );
  }

  private void mockCreateSearchRequest(String profileId, boolean queryable) {
    when(searchProfileService.getSearchProfileByProfileId(profileId))
        .thenReturn(getSearchProfileDto(queryable));
  }

  private SearchProfileDto getSearchProfileDto(boolean queryable) {
    SearchProfileDto searchProfileDto = new SearchProfileDto();
    Analyser analyser = new Analyser();
    analyser.setFaultTolerant(false);

    searchProfileDto.setApplicationId(UUID.randomUUID());
    searchProfileDto.setSearchFields(new LinkedList<>());
    searchProfileDto.setAnalyser(analyser);
    searchProfileDto.setRelativeScore(1.0);
    searchProfileDto.setQueryable(queryable);

    return searchProfileDto;
  }
}
