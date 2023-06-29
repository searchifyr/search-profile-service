package com.github.searchprofileservice.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IndexHelper {

  public static void deleteIndexIfExists(
      ElasticsearchClient elasticsearchClient, String name) throws IOException {

    if (doesIndexExist(elasticsearchClient, name))
      elasticsearchClient.indices().delete(d -> d.index(name));
  }

  public static boolean doesIndexExist(
      ElasticsearchClient elasticsearchClient, String name) throws IOException {
    return elasticsearchClient.indices().exists(e -> e.index(name)).value();
  }

  public static void createIndex(
      ElasticsearchClient elasticsearchClient, String name) throws IOException {

    elasticsearchClient.indices().create(c -> c.index(name));
  }

  public static void insertDocumentIntoIndex(
      ElasticsearchClient elasticsearchClient, String indexName, String json) throws IOException {

    Reader reader = new StringReader(json);
    elasticsearchClient.index(i -> i.index(indexName).withJson(reader));
  }

}
