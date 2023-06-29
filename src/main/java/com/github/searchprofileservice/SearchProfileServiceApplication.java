package com.github.searchprofileservice;

import com.github.cloudyrock.spring.v5.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
/*TODO:
  should live in a persistence configuration component, if more database specific configuration
  is implemented.
*/
@EnableMongoAuditing
@EnableMongock
public class SearchProfileServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SearchProfileServiceApplication.class, args);
  }

}
