package com.github.searchprofileservice.service.impl;

import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

  @Value("${spring.profiles.active:}")
  private String activeProfiles;

  @Override
  public AuthenticatedUser getUser() {
    if (!isUserAuthenticated()) {
      log.error("No user is authenticated. The user does not exist");
      throw new NullPointerException("No user is authenticated. The user does not exist");
    }

    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof DefaultOAuth2User oAuth2User) {
      var userId = oAuth2User.getAttribute("id");
      if (null == userId) {
        // TODO: return more specific (checked) exception
        throw new RuntimeException("UserID must not be null");
      } else {
        var attributeMap = oAuth2User.getAttributes();
        var avatarUrl = attributeMap.getOrDefault("avatar_url", "");
        return new AuthenticatedUser(oAuth2User.getAttribute("login"), userId.toString(), avatarUrl.toString());
      }
    } else if (null != activeProfiles && activeProfiles.contains("test") && principal instanceof User userDetails) {
      /*
      When runing w/ 

      ```
      @SpringBootTest
      @AutoConfigureMockMvc(addFilters = true)
      ```
      
      the user object provided by `@WithMockUser` will be an instance of
      `package org.springframework.security.core.userdetails.User`.
      To test the funcionality offered by `AuthenticationServiceImpl` it is mandatory, that such a
      user object may be handled like a valid user.
      */
      return new AuthenticatedUser(userDetails.getUsername(), "", "");
    } else {
      log.error("Conversion to DefaultOAuth2User failed. Got " + principal);
      throw new RuntimeException("Conversion to DefaultOAuth2User failed.");
    }
  }

  @Override
  public boolean isUserAuthenticated() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }
  
  @Override
  public boolean isAdmin(Authentication authentication) {
      return null != authentication && authentication.getAuthorities()
        .stream()
        .map(a -> a.getAuthority())
        .filter(a -> "ROLE_ADMIN".equals(a))
        .findAny()
        .isPresent();
  }
}
