package com.github.searchprofileservice.config;

import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.persistence.mongo.model.User;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoginUserFilter extends GenericFilterBean {

  private final AuthenticationService authenticationService;
  private final UserService userService;

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws ServletException, IOException {
    if (authenticationService.isUserAuthenticated()) {
      AuthenticatedUser authenticatedUser = authenticationService.getUser();
      Optional<User> user = userService.findByUserId(authenticatedUser.getId());

      if (user.isEmpty()) {
        User newUser = new User(authenticatedUser.getId(), authenticatedUser.getUsername(), authenticatedUser.getPictureLink(), false);
        if(userService.userIsAdmin(newUser)){
          userService.setUserToAdmin(newUser);
          newUser.setActivated(true);
        }
        userService.save(newUser);
      }
      else{
        var updatedUser = user.get();
        updatedUser.setPictureLink(authenticatedUser.getPictureLink());
        userService.update(updatedUser);
      }
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }


}
