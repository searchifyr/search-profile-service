package com.github.searchprofileservice.service.impl;

import com.github.searchprofileservice.api.model.ElasticSearchUser;
import com.github.searchprofileservice.api.model.UserDTO;
import com.github.searchprofileservice.model.enums.Role;
import com.github.searchprofileservice.persistence.mongo.model.User;
import com.github.searchprofileservice.persistence.mongo.repository.UserRepository;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import com.github.searchprofileservice.service.UserService;
import java.util.*;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.passay.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service

public class UserServiceImpl implements UserService {

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ElasticSearchClientService clientService;

  @Value("${spring.security.adminId}")
  private String adminId;

  @Override
  public Optional<User> findByUserId(String userId) {
    return Optional.ofNullable(userRepository.findByUserId(userId));
  }

  @Override
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  @Override
  public Stream<UserDTO.BasicProjection> getAllActiveUsersAsBasicProjections() {
    return userRepository.findAllAsBasicProjection().map(UserDTO.BasicProjection::fromUser);
  }

  @Override
  public User save(User user) {
    if (StringUtils.isBlank(user.getName())) {
      throw new IllegalArgumentException("'Name' can not be null or empty");
    }

    if (null == user.getUserId()) {
      user.setUserId(UUID.randomUUID().toString());
    }

    if (null == user.getPictureLink()) {
      user.setPictureLink("");
    }

    user.setCreatedDate(new Date());

    return userRepository.save(user);
  }

  @Override
  public User update(User user) {
    if (null == user.getUserId()) {
      throw new IllegalArgumentException("'userId' can not be null");
    }
    if (StringUtils.isBlank(user.getName())) {
      throw new IllegalArgumentException("'Name' can not be null or empty");
    }

    return userRepository.save(user);
  }

  @Override
  public void delete(String userId) {
    userRepository.deleteById(userId);
  }

  @Override
  public boolean existsByUserId(String userId) {
    return userRepository.existsById(userId);
  }

  @Override
  public boolean userIsAdmin(User user){
    return user.getUserId().equals(adminId);
  }

  @Override
  public void setUserToAdmin(User user) {
    user.setActivated(true);
    user.setRole(Role.ADMIN);
  }

  @Override
  public boolean createElasticSearchUser(ElasticSearchUser newUser) {
    return clientService.createElasticSearchUser(newUser);
  }

  @Override
  public boolean isPasswordValid(String password) {
    List<Rule> rules = new ArrayList<>();
    //Rule 1: Password length should be in between
    //8 and 16 characters
    rules.add(new LengthRule(8, 16));
    //Rule 2: No whitespace allowed
    rules.add(new WhitespaceRule());
    //Rule 3.a: At least one Upper-case character
    rules.add(new CharacterRule(GermanCharacterData.UpperCase, 1));
    //Rule 3.b: At least one Lower-case character
    rules.add(new CharacterRule(GermanCharacterData.LowerCase, 1));
    //Rule 3.c: At least one digit
    rules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
    //Rule 3.d: At least one special character
    rules.add(new CharacterRule(EnglishCharacterData.Special, 1));

    PasswordValidator validator = new PasswordValidator(rules);
    PasswordData passwordData = new PasswordData(password);
    RuleResult result = validator.validate(passwordData);
    return result.isValid();
  }
}
