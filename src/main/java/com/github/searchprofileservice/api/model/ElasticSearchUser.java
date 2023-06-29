package com.github.searchprofileservice.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ElasticSearchUser {
    private String userName;
    private char[] password;
}
