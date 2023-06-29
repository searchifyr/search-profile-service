package com.github.searchprofileservice.api.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Exchange object for {@link com.github.searchprofileservice.persistence.mongo.model.Application }
 *
 * <p>A presentation of an `Application` as given / presented to the client.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplicationDto {

  private UUID id;

  private Date createdDate;

  private String name;

  private String creatorId;

  private boolean active;

  private List<String> allowedUserIds;

  private List<ApiKeyDto> reducedApiKeys;

  private long numberOfDocuments;

  public ApplicationDto(UUID id,
                        Date createdDate,
                        String name,
                        String creatorId,
                        boolean active,
                        List<String> allowedUserIds,
                        List<ApiKeyDto> reducedApiKeys){

    this.id = id;
    this.createdDate = createdDate;
    this.name = name;
    this.creatorId = creatorId;
    this.active = active;
    this.allowedUserIds = allowedUserIds;
    this.reducedApiKeys = reducedApiKeys;
  }
}
