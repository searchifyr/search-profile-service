package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.routes.Routes.Api.V1.SearchProfiles.GetOne.PathParams;
import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.api.model.validator.SearchProfileValidator;
import com.github.searchprofileservice.api.model.validator.SearchProfileValidator.ValidationResult;
import com.github.searchprofileservice.model.SearchField;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ApplicationService;
import com.github.searchprofileservice.service.SearchProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.searchprofileservice.api.routes.Routes.Api.V1.SearchProfiles.*;

/**
 * Endpoint for CRUD of search profiles.
 *
 * <p>Uses {@link SearchProfileDto} as exchange object between.
 * server and client.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@Secured("ROLE_USER")
public class SearchProfileController {

  private final SearchProfileService searchProfileService;

  private final ApplicationService applicationService;

  /**
   * Returns all existing search profiles.
   *
   * @return HTTP 200
   */
  @GetMapping(path = GetAll)
  public ResponseEntity<List<SearchProfileDto.BasicProjection>> getAllSearchProfiles() {
    List<SearchProfileDto.BasicProjection> searchProfiles =
      searchProfileService.getAllSearchProfilesAsBasicProjection();
    return ResponseEntity.ok(searchProfiles);
  }

  /**
   * Returns the search-profile by the profile-id.
   *
   * @return
   * Http 200 if successful, 400 if given UUID is not a valid UUID,
   * 404 if no search-profile with the given search-profile-id can be found
   */
  @GetMapping(path = GetOne.route)
  public ResponseEntity<SearchProfileDto> getSearchProfileById(
      @PathVariable(PathParams.profileId) String profileId) {

    getUuidFromString(profileId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "'" + profileId + "' is not a valid UUID"));

    final SearchProfileDto searchProfile =
      searchProfileService.getSearchProfileByProfileId(profileId);

    if (null == searchProfile) {
      return ResponseEntity.notFound().build();
    }

    final List<SearchField> fields =
      Optional.ofNullable(searchProfile.getSearchFields()).orElseGet(ArrayList::new);
    final Set<String> currentFieldNames =
      fields.stream().map(field -> field.getFieldName()).collect(Collectors.toSet());
    final Set<String> availableFieldNames = 
      searchProfileService.loadSearchFieldKeysForSearchProfile(searchProfile);

    // remove fields from search profile that are not available by ES
    final Iterator<SearchField> fieldsIterator = fields.iterator();
    while (fieldsIterator.hasNext()) {
      final SearchField field = fieldsIterator.next();
      if (!availableFieldNames.contains(field.getFieldName())) {
        fieldsIterator.remove();
      }
    }

    // add fields available in ES but not yet known to the search profile
    for (final String fieldName : availableFieldNames) {
      if (!currentFieldNames.contains(fieldName)) {
        fields.add(new SearchField(fieldName, false, 1.0));
        currentFieldNames.add(fieldName);
      }
    }

    return ResponseEntity.ok(searchProfile);
  }

  /**
   * Creates new search profile from given request body.
   *
   * @return 201 if successful, 400 if given body not valid or application is not active, 401 if the current user is not authorized,
   * 404 if corresponding application can't be found
   */
  @PostMapping(path = Post)
  public ResponseEntity<SearchProfileDto> postNewSearchProfile(
      @RequestBody SearchProfileDto searchProfileDto
  ) {

    ValidationResult result = SearchProfileValidator.isApplicationIdValid()
        .and(SearchProfileValidator.isNameValid())
        .and(SearchProfileValidator.areFieldsValid())
        .apply(searchProfileDto);

    if (result != ValidationResult.SUCCESS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.getLabel());
    }

    Application application = applicationService.findById(searchProfileDto.getApplicationId()).orElseThrow(() -> new ResponseStatusException(
          HttpStatus.NOT_FOUND, 
          "No application with id: " + searchProfileDto.getApplicationId() + " found."));

    if (!application.isActive()) {
      String exceptionMessage = String.format("The application "
              + "with the id '%s' is not active. Search-profiles can only be created for active "
              + "applications. An application is considered active, if at least one document is part "
              + "of the application.", application.getId());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exceptionMessage);
    }

    if (!applicationService.isEditableByCurrentUser(application)) {
      String exceptionMessage = String.format("The application "
              + "with the id '%s' is not accessible by the current user. Search-profiles can only be created for "
              + "applications the user is allowed to use or applications created by the current user.", application.getId());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exceptionMessage);
    }

    SearchProfileDto searchProfile = searchProfileService.postNewSearchProfile(searchProfileDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(searchProfile);
  }

  /**
   * Updates existing search profile from given request body.
   *
   * @return 200 if update successful, 400 if given UUID is not a valid UUID or if given body is not valid, 401 if the
   * current user is not authorized, 404 if given search profileId or applicationId does not exist
   */
  @PutMapping(path = Put.route)
  public ResponseEntity<SearchProfileDto> updateSearchProfile(
      @RequestBody SearchProfileDto searchProfileDto,
      @PathVariable(Put.PathParams.profileId) String profileId) {

    ValidationResult result = SearchProfileValidator.isApplicationIdValid()
        .and(SearchProfileValidator.isNameValid())
        .and(SearchProfileValidator.areFieldsValid())
        .apply(searchProfileDto);

    if (result != ValidationResult.SUCCESS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.getLabel());
    }

    getUuidFromString(profileId).orElseThrow(() -> new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "'" + profileId + "' is not a valid UUID"));

    Application correspondingApplication = applicationService.findById(searchProfileDto.getApplicationId()).orElseThrow(() -> new ResponseStatusException(
      HttpStatus.NOT_FOUND, "No application with id: " + searchProfileDto.getApplicationId() + " found."));

    if (!applicationService.isEditableByCurrentUser(correspondingApplication)) {
      String exceptionMessage = String.format("The application "
              + "with the id '%s' is not accessible by the current user. Search-profiles can only be updated for "
              + "applications the user is allowed to use or private applications created by the current user.", correspondingApplication.getId());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exceptionMessage);
    }

    SearchProfileDto updatedSearchProfile
        = searchProfileService.updateSearchProfile(searchProfileDto, profileId);
    return ResponseEntity.ok(updatedSearchProfile);
  }

  /**
   * Deletes existing search profile by given profile id.
   *
   * @return HTTP-Response 204 if deletion was successful, 400 if given profileId not a valid UUID, 
   *         401 if current user is not authorized to delete a search profile for the corresponding application, 
   *         404 if no search profile exists for given id
   */
  @DeleteMapping(path = Delete.route)
  public ResponseEntity<Void> deleteSearchProfile(
      @PathVariable(Delete.PathParams.profileId) String profileId
  ) {

    getUuidFromString(profileId).orElseThrow(() -> new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "'" + profileId + "' is not a valid UUID"));

    if (!searchProfileService.existsSearchProfile(profileId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "No search profile exists for the given profileId");
    }

    Optional<Application> correspondingApplication = applicationService.findById(searchProfileService.getSearchProfileByProfileId(profileId).getApplicationId());

    if (!applicationService.isEditableByCurrentUser(correspondingApplication.get())) {
      String exceptionMessage = String.format("The application "
              + "with the id '%s' is not accessible by the current user. Search-profiles can only be deleted for "
              + "applications the user is allowed to use or private applications created by the current user.", correspondingApplication.get().getId());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exceptionMessage);
    }

    searchProfileService.deleteSearchProfile(profileId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Converts the given string into a UUID if it's a valid UUID, if not nothing is returned
   * @param id string which should be converted into a UUID
   * @return Optional with valid UUID or an empty Optional container
   */
  private static Optional<UUID> getUuidFromString(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (Throwable t) {
      log.warn(t.getMessage());
      return Optional.empty();
    }
  }
}
