package com.github.searchprofileservice.persistence.mongo.model.base;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ApiKey {

    private final UUID id;

    private String name;

    private final String key;
}
