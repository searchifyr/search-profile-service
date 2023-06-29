package com.github.searchprofileservice.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiKeyCreatedResponse {

    private String clearTextApiKey;

    private ApiKeyDto apiKey;
}
