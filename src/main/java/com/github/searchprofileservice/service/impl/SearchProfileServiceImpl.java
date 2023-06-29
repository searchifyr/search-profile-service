package com.github.searchprofileservice.service.impl;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.api.model.SearchProfileDto.BasicProjection;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.model.SearchField;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.SearchProfileDocument;
import com.github.searchprofileservice.persistence.mongo.repository.SearchProfileRepository;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import com.github.searchprofileservice.service.SearchProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * implementation of search profile.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchProfileServiceImpl implements SearchProfileService {

  private final SearchProfileRepository searchProfileRepository;

  private final ElasticSearchClientService elasticSearchService;

  private final AuthenticationService authenticationService;

  private static final double DEFAULT_BOOST_VALUE = 1.0;
  private static final boolean DEFAULT_ACTIVE_VALUE = true;

  @Override
  public List<SearchProfileDto> getAllSearchProfiles() {
    return searchProfileRepository
        .findAll()
        .stream()
        .map(SearchProfileDocument::toSearchProfileDto)
        .toList();
  }

  @Override
  public List<SearchProfileDto.BasicProjection> getAllSearchProfilesAsBasicProjection() {
    return searchProfileRepository
        .findAllAsBasicProjection()
        .stream()
        .map(SearchProfileDto.BasicProjection::fromDocument)
        .toList();
  }

  @Override
  public SearchProfileDto postNewSearchProfile(SearchProfileDto searchProfile) {
    String userId = authenticationService.getUser().getId();

    SearchProfileDocument searchProfileDocument = SearchProfileDocument.of(searchProfile);
    searchProfileDocument.setProfileId(UUID.randomUUID().toString());
    searchProfileDocument.setCreatorId(userId);
    searchProfileDocument.setLastEditorId(userId);

    if(searchProfileDocument.getRelativeScore() == null){
      searchProfileDocument.setRelativeScore(0.0);
    }
    if (searchProfileDocument.getMinScore() == null){
      searchProfileDocument.setMinScore(0.0);
    }


    if (searchProfileDocument.getAnalyser() == null)
      searchProfileDocument.setAnalyser(new Analyser());

    if (searchProfileDocument.getSearchFields() == null)
      setMapWithDefaultBoostValues(searchProfileDocument);

    searchProfileDocument = searchProfileRepository.save(searchProfileDocument);
    return searchProfileDocument.toSearchProfileDto();
  }

  @Override
  public List<SearchProfileDto> getAllSearchProfilesByApplicationId(String applicationId) {
    return searchProfileRepository.findAllByApplicationId(applicationId)
        .stream()
        .map(SearchProfileDocument::toSearchProfileDto)
        .toList();
  }
  
  @Override
  public List<SearchProfileDto.BasicProjection>
    getAllSearchProfilesByApplicationIdAsBasicProjection(String applicationId) {
    return searchProfileRepository.findAllByApplicationId(applicationId)
        .stream()
        .map(BasicProjection::fromDocument)
        .toList();
  }

  @Override
  public SearchProfileDto updateSearchProfile(SearchProfileDto searchProfile, String profileId) {
    String userId = authenticationService.getUser().getId();
    SearchProfileDocument searchProfileDocument = searchProfileRepository.findById(profileId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No search profile exists for the given profileId"));

    searchProfileDocument.setLastEditorId(userId);
    searchProfileDocument.setName(searchProfile.getName());
    searchProfileDocument.setSearchFields(searchProfile.getSearchFields());
    searchProfileDocument.setAnalyser(searchProfile.getAnalyser());
    searchProfileDocument.setMinScore(searchProfile.getMinScore());
    searchProfileDocument.setRelativeScore(searchProfile.getRelativeScore());
    searchProfileDocument.setQueryable(searchProfile.isQueryable());
    searchProfileDocument = searchProfileRepository.save(searchProfileDocument);

    return searchProfileDocument.toSearchProfileDto();
  }

  @Override
  public void deleteSearchProfile(String profileId) {
    searchProfileRepository.deleteById(profileId);
  }

  @Override
  public void deleteByApplicationId(UUID applicationId) {
    searchProfileRepository.deleteByApplicationId(applicationId.toString());
  }

  @Override
  public boolean existsSearchProfile(String profileId) {
    return searchProfileRepository.existsById(profileId);
  }

  @Override
  public SearchProfileDto getSearchProfileByProfileId(String profileId) {
    return searchProfileRepository.findById(profileId)
        .map(SearchProfileDocument::toSearchProfileDto)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Search profile with id : "
                + profileId + " does not exist."));
  }

  @Override
  public Set<String> loadSearchFieldKeysForSearchProfile(SearchProfileDto searchProfile) {
    return elasticSearchService.getIndexMapping(searchProfile.getApplicationId().toString())
        .entrySet()
        .stream()
        .filter(entry -> !ElasticSearchMappingType.NOT_SUPPORTED.equals(entry.getValue()))
        .map(entry -> entry.getKey())
        .collect(Collectors.toSet());
  }

  /**
   * sets all fields of corresponding application (elastic search) documents and the assigned
   * default value for the given search profile.
   */
  private void setMapWithDefaultBoostValues(SearchProfileDocument searchProfileDocument) {
    searchProfileDocument.setSearchFields(
        elasticSearchService.getIndexMapping(searchProfileDocument.getApplicationId())
            .entrySet()
            .stream()
            .filter(entry -> !entry.getValue().equals(ElasticSearchMappingType.NOT_SUPPORTED))
            .map(
                entry -> new SearchField(entry.getKey(), DEFAULT_ACTIVE_VALUE, DEFAULT_BOOST_VALUE))
            .toList()

    );
  }
}
