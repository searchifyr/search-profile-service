package com.github.searchprofileservice.service;

import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.service.impl.AuthenticationServiceImpl;
import com.github.searchprofileservice.support.AuthenticationMockClass;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationServiceTest {

  private final AuthenticationService authenticationService = new AuthenticationServiceImpl();

  @Test
  public void isUserAuthenticated_returnTrue() {
    mockSecurityContext();
    boolean isUserAuthenticated = authenticationService.isUserAuthenticated();

    assertThat(isUserAuthenticated, equalTo(Boolean.TRUE));
  }

  @Test
  public void isUserAuthenticated_returnFalse() {
    boolean isUserAuthenticated = authenticationService.isUserAuthenticated();

    assertThat(isUserAuthenticated, equalTo(Boolean.FALSE));
  }

  @Test
  public void getUser_throwRuntimeException() {
    mockSecurityContextAsNull();
    assertThrows(
        RuntimeException.class,
        authenticationService::getUser
    );
  }

  @Test
  public void getUser() {
    mockSecurityContext();
    AuthenticatedUser user = authenticationService.getUser();
    assertThat(user.getUsername(), equalTo("testUser"));
    assertThat(user.getId(), equalTo("1"));
    assertThat(user.getPictureLink(), equalTo("urlToProfilePicture"));
  }

  private void mockSecurityContext() {
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication())
        .thenReturn(new AuthenticationMockClass());
    SecurityContextHolder.setContext(securityContext);
  }
  private void mockSecurityContextAsNull() {
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication())
        .thenReturn(null);
    SecurityContextHolder.setContext(securityContext);
  }
}
