package com.github.searchprofileservice.api.model;

import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.model.SearchField;
import com.github.searchprofileservice.persistence.mongo.model.SearchProfileDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchProfileDto {

  private String profileId;

  private UUID applicationId;
  private String creatorId;
  private String lastEditorId;

  private LocalDateTime lastModifiedDate;
  private String name;

  private Analyser analyser;

  private List<SearchField> searchFields;

  private Double minScore;

  /*
  * Some requirements for result filtering on ES data  can`t be satisfied by returning a simple Elastic Query.
  * For that Reason there exists an additional endpoint which includes logic for more accurate Results.
  * If a SearchProfile needs to use this additional endpoint is expressed with this var.
  */
  private boolean queryable;

  private Double relativeScore;

  /**
   * A reduced projection of a {@link SearchProfileDocument}, containing only basic fields
   */
  public static record BasicProjection(
    String profileId,
    String applicationId,
    String creatorId,
    String lastEditorId,
    LocalDateTime lastModifiedDate,
    String name
  ) {

    /**
     * Construct a new {@link BasicProjection} from a given {@link SearchProfileDocument}
     * @param document the document to construct a BasicProjection upon
     * @return the projection of the given document
     */
    public static BasicProjection fromDocument(SearchProfileDocument document) {
      return new BasicProjection(
        document.getProfileId(),
        document.getApplicationId(),
        document.getCreatorId(),
        document.getLastEditorId(),
        document.getLastModifiedDate(),
        document.getName()
      );
    }
  };
}
