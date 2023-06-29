package com.github.searchprofileservice.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationMockClass implements Authentication {
  private boolean isAuthenticated = true;
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return null;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return new DefaultOAuth2User(null, getAttributesAsMap(), "id");
  }

  @Override
  public boolean isAuthenticated() {
    return isAuthenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    this.isAuthenticated = isAuthenticated;
  }

  @Override
  public String getName() {
    return null;
  }

  private Map<String, Object> getAttributesAsMap() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("login", "testUser");
    map.put("id", 1);
    map.put("avatar_url", "urlToProfilePicture");
    return map;
  }
}
