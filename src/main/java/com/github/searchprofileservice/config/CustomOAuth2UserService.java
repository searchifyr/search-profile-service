package com.github.searchprofileservice.config;

import com.github.searchprofileservice.persistence.mongo.model.User;
import com.github.searchprofileservice.persistence.mongo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Custom Oauth2UserService
 *
 * Is responsible for retrieving a user object from an auth request and extracting authorities
 * from that user object.
 * 
 * @implNote
 * This routine is currently customized to handle GitHub only as OAuth provider. If another
 * provider should be supported it may be necessary to adjust the logic of extrating information
 * from the incoming {@link OAuth2UserRequest}.
 */
@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  /**
   * Key of the 'ID' attribute of an OAuth Userinfo retrieved after authentication
   *
   * Is used for mapping OAuth users to user information stored in the application's database
   */
  private static final String NAME_ATTRIBUTE_KEY = "id";

  @Autowired
  private UserRepository userRepository;

  /**
  * A kind of user authority
  */
  private static enum Authority implements GrantedAuthority {
    /** Denotes that a user has administrative privileges */
    ADMIN,

    /** Denotes that a user has regular privileges  */
    USER;

    @Override
    public String getAuthority() {
      return "ROLE_" + this.name();
    }
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    final OAuth2User oauth2User = super.loadUser(userRequest);
    final Optional<Object> id = Optional.ofNullable(oauth2User.getAttribute(NAME_ATTRIBUTE_KEY));
    final Optional<User> user = id.flatMap(i -> userRepository.findById(i.toString()));

    if (user.isPresent()) {
      Set<GrantedAuthority> authorities = getUserAuthorities(user.get());
      return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), NAME_ATTRIBUTE_KEY);
    } else {
      return oauth2User;
    }
  }

  private Set<GrantedAuthority> getUserAuthorities(User user) {
    if (user.isAdmin()) {
      return Set.of(Authority.ADMIN, Authority.USER);
    } else if (user.isActivated()) {
      return Set.of(Authority.USER);
    } else {
      return Set.of();
    }
  }
}
