package com.github.searchprofileservice.service;

import org.springframework.security.core.Authentication;

import com.github.searchprofileservice.model.AuthenticatedUser;

/**
 * service to extract data from the github token
 */
public interface AuthenticationService {
  /**
   * @return an object, of the userId, userName and tpicture-link, which is extracted from the github token.
   */
  AuthenticatedUser getUser();

  /**
   * @return whether the user is authenticated
   */
  boolean isUserAuthenticated();
  
  /**
   * @return true iff the user obtained from the given {@link Authentication} has the authority `ROLE_ADMIN`
   */
  boolean isAdmin(Authentication authentication);
}
