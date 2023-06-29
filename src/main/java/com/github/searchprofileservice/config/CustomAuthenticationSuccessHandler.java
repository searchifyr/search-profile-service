package com.github.searchprofileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final String redirectUrl;

  private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

  public CustomAuthenticationSuccessHandler(
      @Value("${spring.security.redirect.login.frontend}") String redirectUrl) {
    this.redirectUrl = redirectUrl;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    redirectStrategy.sendRedirect(request, response, redirectUrl);
  }
}
