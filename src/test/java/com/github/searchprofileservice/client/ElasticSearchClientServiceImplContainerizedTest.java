package com.github.searchprofileservice.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.github.searchprofileservice.SearchProfileServiceApplication;
import com.github.searchprofileservice.container.AbstractElasticSearchTestContainer;
import com.github.searchprofileservice.exception.IndexNotFoundException;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import com.github.searchprofileservice.support.JsonDocumentSample;
import com.github.searchprofileservice.util.IndexHelper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = SearchProfileServiceApplication.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class ElasticSearchClientServiceImplContainerizedTest extends AbstractElasticSearchTestContainer {

  private static final Application SAMPLE_APPLICATION = new Application(
          UUID.fromString("888ca534-c38d-4496-8644-e0cc9bb3870d"),
          null,
          null,
          "test-application",
          "0bf8838a-6fde-478a-bca2-240752ee4533",
          false,
          List.of("111"));

  private static final String SAMPLE_DOCUMENT
      = JsonDocumentSample.getElasticSearchIndexJsonDocumentSample();

  @Autowired
  private ElasticSearchClientService sut;

  @Autowired
  private ElasticsearchClient elasticsearchClient;

  @Test
  @SneakyThrows
  void index_mapping_flattens_raw_elastic_search_mapping_correct() {

    // arrange
    deleteIndexIfExists(SAMPLE_APPLICATION);
    createIndex(SAMPLE_APPLICATION);
    insertDocumentIntoIndex(SAMPLE_APPLICATION, SAMPLE_DOCUMENT);

    Map<String, ElasticSearchMappingType> expected = new HashMap<>();
    expected.put("name", ElasticSearchMappingType.TEXT);
    expected.put("certificate.name", ElasticSearchMappingType.TEXT);
    expected.put("certificate.available", ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put("object1.object2.object3.object4.object5", ElasticSearchMappingType.NOT_SUPPORTED);

    // act
    Map<String, ElasticSearchMappingType> resultApplicationInput
        = sut.getIndexMapping(SAMPLE_APPLICATION);

    Map<String, ElasticSearchMappingType> resultIdInput
        = sut.getIndexMapping(SAMPLE_APPLICATION.getId().toString());

    // assert
    assertEquals(expected, resultApplicationInput);
    assertEquals(expected, resultIdInput);
  }

  @Test
  @SneakyThrows
  void throws_index_not_found_exception_if_index_for_flattening_is_not_found() {

    // arrange
    deleteIndexIfExists(SAMPLE_APPLICATION);

    // act && assert
    assertThrows(IndexNotFoundException.class, () -> sut.getIndexMapping(SAMPLE_APPLICATION));
    assertThrows(IndexNotFoundException.class,
        () -> sut.getIndexMapping(SAMPLE_APPLICATION.getId().toString()));
  }

  private void deleteIndexIfExists(Application application) throws IOException {
    IndexHelper.deleteIndexIfExists(elasticsearchClient, application.getId().toString());
  }

  private void createIndex(Application application) throws IOException {
    IndexHelper.createIndex(elasticsearchClient, application.getId().toString());
  }

  private void insertDocumentIntoIndex(Application application, String document) throws IOException {
    IndexHelper.insertDocumentIntoIndex(elasticsearchClient, application.getId().toString(), document);
  }

}