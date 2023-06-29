package com.github.searchprofileservice.persistence.mongo.model;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;

import java.util.*;

/**
 * represents the document of an application object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "application")
public class Application implements Persistable<UUID> {

  @Id
  private UUID id;

  @CreatedDate
  private Date createdDate;

  @Builder.Default
  private List<ApiKey> apiKeys = new ArrayList<>();

  @Indexed(unique = true)
  private String applicationName;

  private String creatorId;

  private boolean active;

  @Builder.Default
  private List<String> allowedUserIds = new ArrayList<>();

  //Reduced constructor used by the ApplicationDTO toApplication() method as api key is not given by frontend
  public Application(
    UUID id,
    Date createdDate,
    String applicationName,
    boolean active,
    List<String> allowedUserIds
  ) {
    this.id = id;
    this.createdDate = createdDate;
    this.applicationName = applicationName;
    this.active = active;
    this.allowedUserIds = allowedUserIds;
    this.apiKeys = new ArrayList<>();
    this.allowedUserIds = new ArrayList<>();
  }

  public Application(
    UUID id,
    Date createdDate,
    ArrayList<ApiKey> apiKeys,
    String applicationName,
    boolean active,
    List<String> allowedUserIds
  ) {
    this.id = id;
    this.createdDate = createdDate;
    //APIkey is hashed using Bcrypt
    this.apiKeys = apiKeys;
    this.applicationName = applicationName;
    this.active = active;
    this.allowedUserIds = allowedUserIds;
  }
  
  /**
   * @return this application's id
   */
  public Optional<UUID> id() {
    return Optional.ofNullable(id);
  }

  /**
   * Adds a new api key to the application
   * @param newApiKey the ApiKey to be saved with already encrypted key
   */
  public void addApiKey(ApiKey newApiKey){
    this.apiKeys.add(newApiKey);
  }

  /**
   * Removes an api key from an application
   * @param apiKey the api key to remove
   */
  public void removeApiKey(ApiKey apiKey){
    this.apiKeys.remove(apiKey);
  }

  @Override
  public boolean isNew() {
    return null == createdDate;
  }
}
