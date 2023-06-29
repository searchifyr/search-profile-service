package com.github.searchprofileservice.api.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@EqualsAndHashCode

public class UserLoginStatus {
  private final boolean authenticated;
  private final String userId;
  private final String userName;
  private final String pictureLink;
  private boolean admin;
  private boolean activated;

  public static UserLoginStatus notAuthenticated() {
    return new UserLoginStatus(false, "", "", "", false, false);
  }
}
