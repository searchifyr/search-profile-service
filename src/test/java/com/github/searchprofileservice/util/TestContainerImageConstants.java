package com.github.searchprofileservice.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestContainerImageConstants {

  public static final DockerImageName ELASTIC_SEARCH_IMAGE_NAME
      = DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.2.2");

  public static final Map<String, String> ELASTIC_SEARCH_ENVIRONMENT_VARIABLES = Map.of(
      "discovery.type",                     "single-node",
      "node.name",                          "es01",
      "cluster.name",                       "escluster01",
      "ELASTIC_PASSWORD",                   "elasticpassword",
      "xpack.security.enabled",             "true",
      "xpack.security.http.ssl.enabled",    "false",
      "xpack.license.self_generated.type",  "basic",
      "ES_JAVA_OPTS",                       "-Xms750m -Xmx750m"
  );
}
