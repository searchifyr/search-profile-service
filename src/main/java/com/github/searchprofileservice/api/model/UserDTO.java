package com.github.searchprofileservice.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.searchprofileservice.model.enums.Role;
import com.github.searchprofileservice.persistence.mongo.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    private String userId;
    private Date createdDate;
    private String name;
    private String pictureLink;
    private boolean activated;
    private Role role;

    public User toUser() {
        return new User(userId, createdDate, name, pictureLink, activated, role);
    }

    public static UserDTO fromUser(User user) {
        return new UserDTO(user.getUserId(), user.getCreatedDate(), user.getName(), user.getPictureLink(), user.isActivated(), user.getRole());
    }
    
    /**
     * Projection of {@link User} consisting only of fields `userId` and `name`
     */
    public record BasicProjection(
        @JsonProperty("userId") String userId,
        @JsonProperty("name") String name
    ) {
        public static BasicProjection fromUser(User user) {
            return new BasicProjection(user.getUserId(), user.getName());
        }
    }
}
