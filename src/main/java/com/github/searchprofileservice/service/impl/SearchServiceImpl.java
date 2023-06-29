package com.github.searchprofileservice.service.impl;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.client.ElasticSearchStatefulClient;
import com.github.searchprofileservice.client.impl.ElasticSearchStatefulClientImpl;
import com.github.searchprofileservice.model.SearchResult;
import com.github.searchprofileservice.model.SearchResults;
import com.github.searchprofileservice.service.SearchProfileService;
import com.github.searchprofileservice.service.SearchService;
import com.github.searchprofileservice.util.ElasticSearchSearchRequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

  private final SearchProfileService searchProfileService;
  private final ElasticSearchStatefulClient elasticSearchStatefulClient;
  private final String placeholderSearchValue = "{{placeholder}}";

  @Override
  public SearchResults search(String profileId, String searchValue){
    SearchProfileDto searchProfileDto = searchProfileService.getSearchProfileByProfileId(profileId);
    return getSearchResults(searchValue, searchProfileDto);
  }

  private SearchResults getSearchResults(String searchValue, SearchProfileDto searchProfileDto) {
    SearchRequest searchRequest = createSearchRequest(searchProfileDto, searchValue);
    SearchResults  results;
    try {
      results = convertToSearchResults(elasticSearchStatefulClient.search(searchRequest));
    }
    catch (IOException e) {
      log.error(e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not perform request on elastic search.");
    }
    if(searchProfileDto.isQueryable() || results.getNumberOfResults() == 0){
      return results;
  }else{
      return getRelativeSearchResults(results, searchProfileDto);
    }
  }

  private SearchResults getRelativeSearchResults(SearchResults searchResults, SearchProfileDto searchProfileDto ){

    var results = searchResults.getResults();
    var relativeScore = searchProfileDto.getRelativeScore();

    double thresholdScore = 0;
    for (SearchResult result : results) {
      if(Objects.equals(result, results.get(0))) {
        continue;
      }
      SearchResult previousResult = results.get(results.indexOf(result) - 1);
      if((previousResult.getScore() - result.getScore()) >= relativeScore){
        thresholdScore = previousResult.getScore();
        break;
      }
    }

    double finalThresholdScore = thresholdScore;
    var filteredResults = searchResults.getResults()
            .stream()
            .filter(result -> result.getScore() >= finalThresholdScore)
            .collect(Collectors.toList());

    searchResults.setNumberOfResults(filteredResults.size());
    searchResults.setResults(filteredResults);
    return searchResults;
  }


  @Override
  public String getSearchQuery(String profileId){
    return getSearchQuery(profileId, placeholderSearchValue);
  }

  @Override
  public String getSearchQuery(String profileId, String searchValue){
    SearchProfileDto searchProfileDto = searchProfileService.getSearchProfileByProfileId(profileId);
    if(searchProfileDto.isQueryable()){
      SearchRequest searchRequest = createSearchRequest(searchProfileDto, searchValue);
      return searchRequest.toString();
    }else{
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,"Searchprofile do not support Elastic Query's. Use additional endpoint for Receiving direct Results");
    }
  }

  @Override
  public SearchResults searchByProfileDTO(SearchProfileDto searchProfileDto, String searchValue) {
    return getSearchResults(searchValue, searchProfileDto);
  }

  private SearchResults convertToSearchResults(SearchResponse<ObjectNode> response) {
    SearchResults searchResults = new SearchResults();
    List<Hit<ObjectNode>> hits = response.hits().hits();

    List<SearchResult> results = hits.stream()
        .map(hit -> new SearchResult(hit.score(), hit.source(), hit.highlight()))
        .toList();

    searchResults.addResults(results);

    return searchResults;
  }

  private SearchRequest createSearchRequest(SearchProfileDto searchProfileDto, String searchValue) {
    ElasticSearchSearchRequestUtil searchRequestUtil = searchProfileDto.getAnalyser().isPartialWordSearch() ?
      new ElasticSearchSearchRequestUtil(searchProfileDto.getApplicationId().toString() + ElasticSearchStatefulClientImpl.PARTIAL_WORD_INDEXNAME_POSTFIX, searchValue) :
      new ElasticSearchSearchRequestUtil(searchProfileDto.getApplicationId().toString(), searchValue);

    return searchRequestUtil.createRequest(
      searchProfileDto.getSearchFields(),
      searchProfileDto.getMinScore(),
      searchProfileDto.getAnalyser());
  }

}
