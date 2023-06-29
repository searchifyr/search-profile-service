package com.github.searchprofileservice.model;

import lombok.Data;

@Data
public class Analyser {
  private boolean faultTolerant = false;

  private boolean partialWordSearch = false;
  
  public boolean isFaultTolerant() {
    return faultTolerant;
  }
}
