package com.github.searchprofileservice.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.github.searchprofileservice.SearchProfileServiceApplication;
import com.github.searchprofileservice.container.AbstractElasticSearchTestContainer;
import com.github.searchprofileservice.util.IndexHelper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SearchProfileServiceApplication.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class ElasticSearchStatefulClientImplContainerizedTest extends AbstractElasticSearchTestContainer {

  @Autowired
  private ElasticSearchStatefulClient sut;

  @Autowired
  private ElasticsearchClient elasticsearchClient;

  @Test
  @SneakyThrows
  void elastic_search_connection_is_available() {

    // act && assert
    assertTrue(sut.isConnectionAvailable());
  }

  @Test
  @SneakyThrows
  void index_in_elastic_search_is_creatable() {

    // arrange
    String indexName = "test-index";
    IndexHelper.deleteIndexIfExists(elasticsearchClient, indexName);

    // act
    boolean successful = sut.createIndex(indexName);

    // assert
    assertTrue(successful);
    assertTrue(IndexHelper.doesIndexExist(elasticsearchClient, indexName));
  }
}