package com.github.searchprofileservice.api.model;

import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;


@Data
@AllArgsConstructor
public class ApiKeyDto {

    private final UUID id;

    private String name;

    public static ApiKeyDto fromApiKey (ApiKey apiKey){
        return new ApiKeyDto(
                apiKey.getId(),
                apiKey.getName()
        );
    }

}
