package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.SearchResults;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ApplicationService;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static com.github.searchprofileservice.api.routes.Routes.Api.V1.search.query;
import static com.github.searchprofileservice.api.routes.Routes.Api.V1.search.searchresults;
import static com.github.searchprofileservice.api.routes.Routes.Api.V1.search.test.Post;

@Slf4j
@Controller
@RequiredArgsConstructor
@Secured("ROLE_USER")
public class SearchController {

  private final SearchService searchService;
  private final AuthenticationService authenticationService;
  private final ApplicationService applicationService;

  /**
   * Executes a search query from a search profile with elastic search
   *
   * @return
   *  200, if update successful
   *  400, if given search value is not valid
   *  404, if given search profile id does not exist
   */
  @GetMapping(path = searchresults.get)
  public ResponseEntity<SearchResults> getSearchResults(@PathVariable String profileId,
      @RequestParam("value") String searchValue) {

    if (StringUtils.isBlank(searchValue)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Value should not be null or empty."
      );
    }

    getUuidFromString(profileId).orElseThrow(() -> new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "'" + profileId + "' is not a valid UUID"));

    SearchResults search = searchService.search(profileId, searchValue);
    return ResponseEntity.ok(search);
  }

  /**
   * Returns a search query from search profile
   *
   * @return
   *  200, if update successful
   *  400, if given search value is not valid
   *  404, if given search profile id does not exist
   */
  @GetMapping(path = query.get2)
  public ResponseEntity<String> getQuery(@PathVariable String profileId) {
    getUuidFromString(profileId).orElseThrow(() -> new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "'" + profileId + "' is not a valid UUID"));

    String query = searchService.getSearchQuery(profileId);
    return ResponseEntity.ok(query);
  }

  /**
   * Uses unsaved SearchProfileDTO to execute a search query
   *
   * @return
   *  200, returns query results
   *  400, Given ApplicationID is not correct.
   *  403, You are not allowed to use this application.
   */
  @PostMapping(path = Post)
  public ResponseEntity<SearchResults> postTestSearchProfile
    (@RequestBody SearchProfileDto searchProfileDto,
    @RequestParam("value") String searchValue){
    Optional<Application> application = applicationService.findById(searchProfileDto.getApplicationId());
    if (application.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application does not exist!");
    }
    String userid = authenticationService.getUser().getId();
    if (!userid.equals(application.get().getCreatorId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to use this application.");
    }

    SearchResults searchResults = searchService.searchByProfileDTO(searchProfileDto, searchValue);
    return ResponseEntity.ok(searchResults);
  }


  /**
   * Converts the given string into a UUID if its a valid UUID, if not nothing is returned
   * @param id string which should be converted into a UUID
   * @return Optional with valid UUID or a empty Optional container
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
