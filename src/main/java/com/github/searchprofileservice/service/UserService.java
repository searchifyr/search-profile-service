package com.github.searchprofileservice.service;

import com.github.searchprofileservice.api.model.ElasticSearchUser;
import com.github.searchprofileservice.api.model.UserDTO;
import com.github.searchprofileservice.persistence.mongo.model.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserService {

    /**
     *
     * @param id UUID of the user
     * @return returns the user if found
     */
    Optional<User> findByUserId(String id);

    /**
     *
     * @return all users
     */
    List<User> getAllUsers();

    /**
     * @return all active users containing only the fields `userId` and `name`
     */
    Stream<UserDTO.BasicProjection> getAllActiveUsersAsBasicProjections();

    /**
     *
     * @param user saves a user
     * @return returns the saved user
     */
    User save(User user);

    /**
     *
     * @param user updates a user
     * @return returns the updated user
     */
    User update(User user);

    /**
     *
     * @param uuid deletes the user by the given UUID
     */
    void delete(String uuid);



    /**
     * @return whether the user is created In ElasticsearchInstance
     */
    boolean createElasticSearchUser(ElasticSearchUser newUser);

    /**
     * Password requirements:
     *  a digit must occur at least once,
     * a lower case letter must occur at least once,
     * an upper case letter must occur at least once,
     * a special character must occur at least once,
     * no whitespace allowed in the entire string,
     * anything, at least eight places though
     * @param password password as String
     * @return whether the password matches the validator rules
     */
    boolean isPasswordValid(String password);

    boolean existsByUserId(String uuid);

    /**
     * Checks if the User.userId is the admin userid
     * @param user the user to check
     * @return if the user is admin
     */
    boolean userIsAdmin(User user);

    /**
     * Sets the given user to Admin
     * @param user the user who is admin
     */
    void setUserToAdmin(User user);

}
