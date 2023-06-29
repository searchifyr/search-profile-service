package com.github.searchprofileservice.support;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.model.Analyser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchProfileUtil {

  public static SearchProfileDto getSearchProfileDtoWithAllParams() {
    return SearchProfileDto.builder()
        .applicationId(UUID.fromString("ff1107e9-cf66-471d-8eb7-d69e2ef42ee2"))
        .creatorId(SecurityDummyUser.USER_ID)
        .analyser(new Analyser())
        .lastEditorId(SecurityDummyUser.USER_ID)
        .name("test profile")
        .build();
  }

}
