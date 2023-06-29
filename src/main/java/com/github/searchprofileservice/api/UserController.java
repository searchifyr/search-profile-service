package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.model.ElasticSearchUser;
import com.github.searchprofileservice.api.model.UserDTO;
import com.github.searchprofileservice.api.model.UserLoginStatus;
import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.model.enums.Role;
import com.github.searchprofileservice.persistence.mongo.model.User;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.searchprofileservice.api.routes.Routes.Api.V1.Users.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class UserController {

  private final UserService userService;
  private final AuthenticationService authenticationService;

  /**
   * Returns all existing users
   *
   * @param activated optional parameter, if null returns all users, if false only not activated users, if true only activated users
   *
   * @return
   * 200 on success
   */
  @GetMapping(path = GetAll)
  @Secured("ROLE_USER")
  public ResponseEntity<List<?>> getAllUsers(
    @RequestParam("activated") Optional<Boolean> activated,
    Authentication authentication
  ) {
    final boolean isAdmin = authenticationService.isAdmin(authentication);

    if (isAdmin) {
      final List<UserDTO> users = 
        userService.getAllUsers()
          .stream()
          .filter(u ->
            activated.isEmpty() || null == activated.get() || u.isActivated() == activated.get())
          .map(UserDTO::fromUser)
          .toList();

      return ResponseEntity.ok(users);
    } else {
      final List<UserDTO.BasicProjection> users = 
        userService.getAllActiveUsersAsBasicProjections()
          .toList();
      return ResponseEntity.ok(users);
    }
  }

  /**
   * Returns one user
   *
   * @return
   * 200 if success
   * 404 if there is no user with this id
   */
  @GetMapping(path = GetOne.route)
  public ResponseEntity<UserDTO> getUser(@PathVariable(GetOne.PathParams.userId) String userId) {

    Optional<UserDTO> userDTO =
        userService.findByUserId(userId).map(UserDTO::fromUser);

    if (userDTO.isPresent()) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(userDTO.get());
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found for given UUID.");
    }
  }

  /**
   * Deletes a user for a given userID
   *
   * @return
   * 204 if success
   * 404 if no user found for given UUID
   */
  @DeleteMapping(path = GetOne.route)
  public ResponseEntity<Void> deleteUser(@PathVariable(GetOne.PathParams.userId) String userId) {

    if (!userService.existsByUserId(userId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found for given Id.");
    }

    userService.delete(userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
  }

  /**
   * Updates an existing user
   *
   * @param userId  the id of the user to update
   * @param userDTO the updated user
   * @return
   * 200 on success
   * 404 on non-existing ID
   * 400 on missing fields or if given UUID is not a valid UUID
   */
  @PutMapping(path = GetOne.route)
  public ResponseEntity<UserDTO> updateUser(
      @PathVariable(GetOne.PathParams.userId) String userId,
      @RequestBody UserDTO userDTO) {
    if (StringUtils.isBlank(userDTO.getName())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "'name' must not be null or empty.");
    }

    Optional<User> maybeUser = userService.findByUserId(userId);
    if (maybeUser.isPresent()) {
      User user = maybeUser.get();
      user.setName(userDTO.getName());
      user = userService.update(user);
      return ResponseEntity.status(HttpStatus.OK)
          .body(UserDTO.fromUser(user));
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found for given UUID");
    }
  }

/**
 * Updates an existing user and changes the activated-status to true/false
 * @param userId  the id of the user to update
 * @param activated if set to true, the user is getting activated; if set to false, the user is getting deactivated
 * @return
 * 200 on success
 * 403 when trying to deactivate admin
 * 404 when no user with this id exists
 */
  @PutMapping(path = GetOne.activate)
  public ResponseEntity<UserDTO> activateUser (
    @PathVariable (GetOne.PathParams.userId) String userId,
    @RequestParam ("activate") boolean activated
  ) {
    Optional<User> maybeUser = userService.findByUserId(userId);

    if (maybeUser.isPresent() && maybeUser.get().getRole() == Role.ADMIN && !activated) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can't deactivate an admin!");
    }

    if (maybeUser.isPresent()) {
      User user = maybeUser.get();
      user.setActivated(activated);
      userService.save(user);
      return ResponseEntity.status(HttpStatus.OK)
              .body(UserDTO.fromUser(user));
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found for given ID");
    }

}

  /**
   * @return HTTP 200 - Is the user authenticated (via {@link UserLoginStatus} object as content)
   */
  @GetMapping(path = Login.Status)
  @Secured({})
  public ResponseEntity<UserLoginStatus> isUserLoggedIn() {
    if (authenticationService.isUserAuthenticated()) {
      AuthenticatedUser user = authenticationService.getUser();

      Optional<User> dbEntryOfUser = userService.findByUserId(user.getId());
      boolean userIsActivated = false;
      if (dbEntryOfUser.isPresent()){
        userIsActivated = dbEntryOfUser.get().isActivatedUser();
      }

      var userStatus = new UserLoginStatus(true, user.getId(), user.getUsername(), user.getPictureLink(), false, userIsActivated);

      if (dbEntryOfUser.isPresent()) {
        if (dbEntryOfUser.get().getRole().equals(Role.ADMIN))
          userStatus.setAdmin(true);
        else
          userStatus.setAdmin(false);
      }
      return ResponseEntity.ok(userStatus);
    }
    return ResponseEntity.ok(UserLoginStatus.notAuthenticated());
  }

  /**
   * Creates a new User in ElasticSearch Instance
   *
   * @param newUser  for ElasticSearch
   * @return
   * 201 on success
   * 400 on ElasticSearch User Creation failed
   */
  @PostMapping(path = Create)
  public ResponseEntity<ElasticSearchUser> createElasticSearchUser(@RequestBody ElasticSearchUser newUser){
    if (StringUtils.isBlank(newUser.getUserName())){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'username' must not be null or empty.");
    }

    if (newUser.getPassword() == null || !userService.isPasswordValid(String.valueOf(newUser.getPassword()))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"'password' does not match password requirements.Minimum eight characters, at least one uppercase letter, one lowercase letter, one number and one special character");
    }

    if(!userService.createElasticSearchUser(newUser)){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creation of ElasticSearchUser failed");
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
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
