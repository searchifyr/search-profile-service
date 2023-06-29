package com.github.searchprofileservice.service;

import com.github.searchprofileservice.api.model.SearchProfileDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SearchProfileService {

  /**
   * @return all search profiles
   */
  List<SearchProfileDto> getAllSearchProfiles();

  /**
   * @return all search profiles w/ only their basic fields
   */
  List<SearchProfileDto.BasicProjection> getAllSearchProfilesAsBasicProjection();

  /**
   * @return all search profiles with given application id
   */
  List<SearchProfileDto> getAllSearchProfilesByApplicationId(String applicationId);

  /**
   * @return all search profiles with given application id, containing only their basic fields
   */
  List<SearchProfileDto.BasicProjection>
    getAllSearchProfilesByApplicationIdAsBasicProjection(String applicationId);

  /**
   * @return search profiles with given profile id
   */
  SearchProfileDto getSearchProfileByProfileId(String profileId);

  /**
   * creates a new search profile with given data
   *
   * @return profile dto with new profile id
   */
  SearchProfileDto postNewSearchProfile(SearchProfileDto searchProfile);

  /**
   * updates existing search profile with given data
   *
   * @return updated search profile
   */
  SearchProfileDto updateSearchProfile(SearchProfileDto searchProfile, String profileId);

  /**
   * deletes existing search profile by profileId
   */
  void deleteSearchProfile(String profileId);

  /**
   * deletes all search profile document's w/ a give applicationId
   * @param applicationId the id of the application whose search profiles should be deleted
   */
  void deleteByApplicationId(UUID applicationId);

  /**
   * @return true, if search profile with given id exists
   */
  boolean existsSearchProfile(String profileId);
  
  /**
   * Loads available search field keys from ElasticSearch
   * @param searchProfile the search profile for which to load fields
   * @return All search fields available by ElasticSearch
   */
  Set<String> loadSearchFieldKeysForSearchProfile(SearchProfileDto searchProfile);
}
