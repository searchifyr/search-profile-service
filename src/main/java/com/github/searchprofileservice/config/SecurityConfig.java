package com.github.searchprofileservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
  prePostEnabled = true,
  securedEnabled = true,
  jsr250Enabled = true
)
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private static final List<String> ALLOW_EVERYTHING = Collections.singletonList("*");

  private final LoginUserFilter loginUserFilter;

  private final AuthenticationSuccessHandler authenticationSuccessHandler;

  private final CsrfTokenRepository csrfTokenRepository;

  @Autowired
  private CustomOAuth2UserService oAuth2UserService;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http
        .authorizeRequests(a -> a
            .antMatchers(
                // antMatcher area for urls allowed to use by everyone
                "/api/v1/externalServices/query",
                "/api/v1/externalServices/applications/{applicationId}/documents",
                "/api/v1/externalServices/applications/{applicationId}/documents/{documentId}",
                "/api/v1/externalServices/applications/{applicationId}/documents/bulk-upload"    
            ).permitAll()
            .antMatchers(
                // redirect endpoint to frontend
                "/",

                // OpenAPI documentation
                "/api/v1/doc",
                "/api/v1/swagger-ui/*",
                "/v3/api-docs/*",
                "/openapi.yml",

                // Check for user authentication status
                "/api/v1/users/login/status"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(
            HttpStatus.UNAUTHORIZED)))

        .csrf(c ->
          c.csrfTokenRepository(csrfTokenRepository)
            .ignoringRequestMatchers(r -> {
              String origin = r.getHeader("origin");
              return null != origin && origin.startsWith("http://localhost");
            })
        )

        .cors().configurationSource(new CorsConfigurationSource() {
          @Override
          public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
            CorsConfiguration config = new CorsConfiguration();

            config.setAllowedOrigins(
              List.of("http://localhost:8080", "https://<url>", "https://dev.<url>"));
            config.setAllowedMethods(ALLOW_EVERYTHING);
            config.setAllowCredentials(true);
            config.setAllowedHeaders(ALLOW_EVERYTHING);

            return config;
          }
        }).and()
        .logout(l -> l.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)))
        .addFilterAfter(loginUserFilter, OAuth2LoginAuthenticationFilter.class)
        .oauth2Login()
        .successHandler(authenticationSuccessHandler)
        .userInfoEndpoint()
        .userService(oAuth2UserService);
  }
}