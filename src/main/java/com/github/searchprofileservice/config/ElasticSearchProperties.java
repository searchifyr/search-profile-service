package com.github.searchprofileservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class ElasticSearchProperties {

  private int port;
  private String host;
  private boolean ssl;
  private String username;
  private String password;

  @Autowired
  public ElasticSearchProperties(
      @Value("${elasticsearch.connection.port}") int port,
      @Value("${elasticsearch.connection.host}") String host,
      @Value("${elasticsearch.connection.ssl}") boolean ssl,
      @Value("${elasticsearch.connection.username}") String username,
      @Value("${elasticsearch.connection.password}") String password
  ) {
    this.port = port;
    this.host = host;
    this.ssl = ssl;
    this.username = username;
    this.password = password;
  }
}
