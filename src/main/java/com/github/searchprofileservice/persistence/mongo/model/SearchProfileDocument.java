package com.github.searchprofileservice.persistence.mongo.model;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.model.SearchField;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(collection = "search_profile")
@Getter
@Setter
@Builder
public class SearchProfileDocument {

  @Id
  private String profileId;

  private String applicationId;
  private String creatorId;
  private String lastEditorId;

  @LastModifiedDate
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
  private Boolean queryable;
  private Double relativeScore;

  public SearchProfileDto toSearchProfileDto() {
    return SearchProfileDto.builder()
        .profileId(profileId)
        .applicationId(UUID.fromString(applicationId))
        .creatorId(creatorId)
        .lastEditorId(lastEditorId)
        .lastModifiedDate(lastModifiedDate)
        .name(name)
        .analyser(analyser)
        .searchFields(searchFields)
        .minScore(minScore)
        .relativeScore(relativeScore)
            .queryable(this.isQueryable())
            .relativeScore(relativeScore)
        .build();
  }

  public static SearchProfileDocument of(SearchProfileDto searchProfileDto) {
    return SearchProfileDocument.builder()
        .applicationId(searchProfileDto.getApplicationId().toString())
        .creatorId(searchProfileDto.getCreatorId())
        .lastEditorId(searchProfileDto.getLastEditorId())
        .name(searchProfileDto.getName())
        .analyser(searchProfileDto.getAnalyser())
        .searchFields(searchProfileDto.getSearchFields())
        .minScore(searchProfileDto.getMinScore())
        .queryable(searchProfileDto.isQueryable())
        .relativeScore(searchProfileDto.getRelativeScore())
        .build();
  }

  public boolean isQueryable() {
    if (null == this.queryable) {
      return null == this.relativeScore;
    } else {
      return this.queryable;
    }
  }
}
