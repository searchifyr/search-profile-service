package com.github.searchprofileservice.service;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.SearchResults;

public interface SearchService {
  /**
   * creates a query from the given search profile id and executes a search on elastic search
   *
   * @return results of search
   */
  SearchResults search(String profileId, String searchValue);

  /**
   * creates a query from the given search profile id and the search value
   *
   * @return created query as string
   */
  String getSearchQuery(String profileId, String searchValue);

  /**
   * creates a query from the given search profile id with a PlaceHolder search value
   *
   * @return created query as string
   */
  String getSearchQuery(String profileId);

  /**
   * creates a query from the given search profile DTO and the search value
   *
   * @return results of search
   */
  SearchResults searchByProfileDTO(SearchProfileDto searchProfileDto, String searchValue);
}
