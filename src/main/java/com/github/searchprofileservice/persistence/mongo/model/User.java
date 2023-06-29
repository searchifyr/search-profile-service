package com.github.searchprofileservice.persistence.mongo.model;

import com.github.searchprofileservice.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
* Represents a User with privileges.
  */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user")
public class User {
    @Id
    private String userId;

    @CreatedDate
    private Date createdDate;

    private String name;

    private String pictureLink;

    @Setter
    private boolean activated = false;

    private Role role;

    public User(String userId, String userName, String pictureLink, boolean activated) {
        this.userId = userId;
        this.name = userName;
        this.pictureLink = pictureLink;
        this.role = Role.USER;
        this.activated = activated;
    }

    public boolean isActivatedUser(){
        return this.activated;
    }

    public boolean isAdmin(){
        return this.role == Role.ADMIN;
    }
}

