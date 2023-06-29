package com.github.searchprofileservice.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplicationCreatedResponse {

  private String clearTextApiKey;

  private ApplicationDto applicationDto;

}
