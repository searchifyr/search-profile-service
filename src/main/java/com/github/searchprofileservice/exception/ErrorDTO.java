package com.github.searchprofileservice.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorDTO {
  private final LocalDateTime date = LocalDateTime.now();
  private final String message;

  public ErrorDTO(String message) {
    this.message = message;
  }
}
