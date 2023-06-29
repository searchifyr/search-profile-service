package com.github.searchprofileservice.model;

import lombok.Value;

@Value
public class AuthenticatedUser {
  private final String username;
  private final String id;
  private final String pictureLink;
}
