package com.github.searchprofileservice.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SearchResult {

  private double score;
  private JsonNode document;
  Map<String, List<String>> highlight;
}
