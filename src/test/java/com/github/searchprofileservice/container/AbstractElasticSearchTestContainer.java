package com.github.searchprofileservice.container;

import com.github.searchprofileservice.util.TestContainerImageConstants;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract class, which is intended to be used for tests, in which a local elasticsearch
 * instance is needed for testing.
 *
 * Simply extending a test class with this abstract class will provide an elasticsearch instance,
 * as long an application context is created in the test.
 *
 * The correct port configuration will be done automatically on test container startup in the application
 * context.
 */
@Testcontainers
@ActiveProfiles("es-test")
public abstract class AbstractElasticSearchTestContainer {

  @Container
  static GenericContainer ELASTIC_SEARCH_CONTAINER
      = new GenericContainer(TestContainerImageConstants.ELASTIC_SEARCH_IMAGE_NAME)
      .withEnv(TestContainerImageConstants.ELASTIC_SEARCH_ENVIRONMENT_VARIABLES)
      .withReuse(true)
      .withExposedPorts(9200);

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("elasticsearch.connection.port", ELASTIC_SEARCH_CONTAINER::getFirstMappedPort);
  }

}
