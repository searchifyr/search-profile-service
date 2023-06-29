package com.github.searchprofileservice.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class SearchResults {

  private int numberOfResults = 0;
  private List<SearchResult> results = new LinkedList<>();

  public void addResults(Collection<SearchResult> collection) {
    results.addAll(collection);
    numberOfResults += collection.size();
  }
}
