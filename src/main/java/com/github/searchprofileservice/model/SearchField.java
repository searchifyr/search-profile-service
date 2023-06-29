package com.github.searchprofileservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SearchField {
  private String fieldName;
  private boolean enabled;
  private Double boost;
}