package com.github.searchprofileservice.client.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.github.searchprofileservice.client.ElasticSearchStatefulClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.security.*;
import org.elasticsearch.client.security.user.User;
import org.elasticsearch.client.security.user.privileges.IndicesPrivileges;
import org.elasticsearch.client.security.user.privileges.Role;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchStatefulClientImpl implements ElasticSearchStatefulClient {

  private final ElasticsearchClient lowLevelClient;
  private final RestHighLevelClient highLevelClient;

  private static final int MIN_SUBWORD_SIZE = 3;
  public static final String PARTIAL_WORD_INDEXNAME_POSTFIX = "_partial_word";

  @Override
  public GetUsersResponse getAllUser() throws IOException {
    GetUsersRequest request = new GetUsersRequest();
    GetUsersResponse response = highLevelClient.security().getUsers(request, RequestOptions.DEFAULT);
    return response;
  }

  @Override
  public GetRolesResponse getRole(String role) throws IOException{
    GetRolesRequest request = new GetRolesRequest(role);
    GetRolesResponse response = highLevelClient.security().getRoles(request, RequestOptions.DEFAULT);
    return response;
  }

  @Override
  public PutRoleResponse putRole(String roleName, String[] clusterPrivilege, ArrayList<IndicesPrivileges> indicesPrivileges) throws IOException{
    Role role = Role.builder()
            .name(roleName)
            .clusterPrivileges(clusterPrivilege )//Role.ClusterPrivilegeName.ALL_ARRAY
            .indicesPrivileges(indicesPrivileges)
            .build();
    PutRoleRequest request = new PutRoleRequest(role, RefreshPolicy.NONE);
    PutRoleResponse response = highLevelClient.security().putRole(request, RequestOptions.DEFAULT);
    return response;
  }

  @Override
  public PutUserResponse createElasticUser(String userName, char[] password) throws IOException{
    User user = new User(userName, Collections.singletonList("none"));
    PutUserRequest request = PutUserRequest.withPassword(user, password, true, RefreshPolicy.NONE);
    PutUserResponse response = highLevelClient.security().putUser(request, RequestOptions.DEFAULT);
    return response;
  }


  @Override
  public boolean isConnectionAvailable() throws IOException {
    return lowLevelClient.ping().value();
  }

  @Override
  public boolean createIndex(final String name) throws IOException {
    CreateIndexResponse responseCustomAnalyser = lowLevelClient.indices().create(c -> c.index(name + PARTIAL_WORD_INDEXNAME_POSTFIX).settings(getCustomPartialWordAnalyzerSettings()));
    CreateIndexResponse responseStandardAnalyser = lowLevelClient.indices().create(c -> c.index(name));

    return (responseCustomAnalyser.acknowledged() && responseStandardAnalyser.acknowledged());
  }

  @Override
  @CacheEvict(cacheNames="index-mapping", key="#name")
  public void deleteIndex(final String name) throws IOException {
    DeleteIndexResponse responseStandardIndex = lowLevelClient.indices().delete(i -> i.index(name + PARTIAL_WORD_INDEXNAME_POSTFIX)); // delete index with custom analyser
    DeleteIndexResponse responsePartialWord = lowLevelClient.indices().delete(i -> i.index(name)); // delete standard index

    responseStandardIndex.acknowledged();
    responsePartialWord.acknowledged();
  }

  @Override
  @CacheEvict(cacheNames="index-mapping", key="#indexName")
  public String uploadRawJsonToIndex(String indexName, String json) throws IOException {
    Reader input = new StringReader(json);
    String id = makeIndexRequest(IndexRequest.of(i -> i.index(indexName + PARTIAL_WORD_INDEXNAME_POSTFIX).withJson(input))).id(); // make index request on index with custom partial word search analyser
    input.reset();
    makeIndexRequest(IndexRequest.of(i -> i.index(indexName).id(id).withJson(input))); // make index request on standard index
    return id;
  }

  @Override
  @CacheEvict(cacheNames="index-mapping", key="#indexName")
  public void updateDocument(String indexName, String documentId, String json) throws IOException {
    Reader input = new StringReader(json);
    makeIndexRequest(IndexRequest.of(i -> i.index(indexName + PARTIAL_WORD_INDEXNAME_POSTFIX).id(documentId).withJson(input))); // make index request on index with custom partial word search analyser
    input.reset();
    makeIndexRequest(IndexRequest.of(i -> i.index(indexName).id(documentId).withJson(input))); // make index request on standard index
  }

  @Override
  @CacheEvict(cacheNames="index-mapping", key="#indexName")
  public List<String> bulkUploadRawJsonToIndex(String indexName, List<String> jsonDocuments) throws IOException {
    BulkResponse bulkResponsePartialWord = makeBulkUploadRawJsonToIndexRequest(indexName + PARTIAL_WORD_INDEXNAME_POSTFIX, jsonDocuments); // make bulk index request on index with custom analyser
    List<String> uploadedDocumentIds = getUploadedDocumentIds(bulkResponsePartialWord);
    makeBulkUploadRawJsonToIndexRequest(indexName, jsonDocuments, uploadedDocumentIds); // make bulk index request on standard index

    return uploadedDocumentIds;
  }

  /**
   * Makes a bulk index request on a {@code indexName} with multiple {@code jsonDocuments}
   * 
   * @param indexName Name of index to upload jsons
   * @param jsonDocuments List of (json) documents to upload to a index
   * @return The corresponding {@code BulkResponse} of the created bulk request
   * @throws IOException when bulk index request was not successfull or both lists don't have the same length.
   */
  private BulkResponse makeBulkUploadRawJsonToIndexRequest(String indexName, List<String> jsonDocuments) throws IOException {
    BulkRequest request = new BulkRequest();
    for (String jsonDocument : jsonDocuments) {
      var indexRequest = createIndexRequest(indexName, jsonDocument);
      request.add(indexRequest);
    }
    return highLevelClient.bulk(request, RequestOptions.DEFAULT);
  }

  /**
   * Makes a bulk index request on a {@code indexName} with multiple {@code jsonDocuments}
   * 
   * @param indexName Name of index to upload jsons
   * @param jsonDocuments List of (json) documents to upload to a index
   * @param documentIds Not null list of document ids to set the ids of the created documents in the index. List must have same length as list of {@code jsonDocuments}.
   * @return The corresponding {@code BulkResponse} of the created bulk request
   * @throws IOException when bulk index request was not successfull or both lists don't have the same length.
   */
  private BulkResponse makeBulkUploadRawJsonToIndexRequest(String indexName, List<String> jsonDocuments, @NotNull List<String> documentIds) throws IOException {
    if (jsonDocuments.size() != documentIds.size()) {
      throw new IOException("Lists jsonDocuments and documentIds don't have the same length.");
    }
    Iterator<String> jsonDocumentsIter = jsonDocuments.iterator();
    Iterator<String> documentIdsIter = documentIds.iterator();

    BulkRequest request = new BulkRequest();
    while (jsonDocumentsIter.hasNext() && documentIdsIter.hasNext()) {
      var indexRequest = createIndexRequest(indexName, jsonDocumentsIter.next(), documentIdsIter.next());
      request.add(indexRequest);
    }
    return highLevelClient.bulk(request, RequestOptions.DEFAULT);
  }

  /**
   * Makes a index request and returns it response if successfull
   * 
   * @param indexRequest the Index Request to call
   * @return corresponding response of the request
   * @throws ElasticsearchException when index request was not successfull
   * @throws IOException when index request was not successfull
   */
  private <T> IndexResponse makeIndexRequest(IndexRequest<T> indexRequest) throws ElasticsearchException, IOException {
    return lowLevelClient.index(indexRequest);
  }

  /*
  * @return List of successful uploaded DocumentIds
  *   Addition: if a Document was not uploaded successfully to the Elastic Index, the List contains a string error-message to identify the failed upload
  */
  private List<String> getUploadedDocumentIds(BulkResponse response){
    List<String> successfulDocIds = new ArrayList<>();
    for(BulkItemResponse itemResponse : response.getItems()){
      if(itemResponse.getResponse().getResult().getLowercase().equals("created")){
        successfulDocIds.add(itemResponse.getResponse().getId());
      }else{
        successfulDocIds.add(String.format("Could not upload document %s", itemResponse.getId()));
      }
    }
    return successfulDocIds;
  }

  /**
   * Builds an {@code IndexRequest} object for a {@code indexName} with a {@code jsonString} source
   * @param indexName Index where request should be called
   * @param jsonString Json to set requests source
   * @return {@code IndexRequest}
   */
  private org.elasticsearch.action.index.IndexRequest createIndexRequest(String indexName, String jsonString){
    org.elasticsearch.action.index.IndexRequest request = new org.elasticsearch.action.index.IndexRequest(indexName);
    request.source(jsonString, XContentType.JSON);
    return request;
  }

  /**
   * Builds an {@code IndexRequest} object for a {@code indexName} with a {@code jsonString} source
   * @param indexName Index where request would be called
   * @param jsonString Json to set requests source
   * @param documentId
   * @return {@code IndexRequest}
   */
  private org.elasticsearch.action.index.IndexRequest createIndexRequest(String indexName, String jsonString, String documentId){
    org.elasticsearch.action.index.IndexRequest request = new org.elasticsearch.action.index.IndexRequest(indexName);
    request.source(jsonString, XContentType.JSON);
    request.id(documentId);
    return request;
  }

  @Override
  public Map<String, Property> getIndexMapping(String indexName) throws IOException {
    return lowLevelClient.indices()
        .getMapping(b -> b.index(indexName)).get(indexName).mappings().properties();
  }

  @Override
  public boolean isIndexExistent(String indexName) throws IOException {
    return lowLevelClient.indices().exists(b -> b.index(indexName)).value();
  }

  @Override
  public SearchResponse<ObjectNode> search(SearchRequest request) throws IOException {
    return lowLevelClient.search(request, ObjectNode.class);
  }
 
  @Override
  public IndexSettings getCustomPartialWordAnalyzerSettings() {
    // paths in the elasticsearch container image relativ to workDir /usr/share/elasticsearch/
    String pathToWordList = "hyphenation/dictionary-de.txt"; 
    String pathToXMLFile = "hyphenation/de_DR.xml";
    
    return IndexSettings.of(q -> q
    .numberOfShards("1")
    .analysis(p -> p

      .filter("german_partial_word_search", z -> z
      .definition(t -> t
        .hyphenationDecompounder(r -> r
          .wordListPath(pathToWordList)
          .hyphenationPatternsPath(pathToXMLFile)
          .onlyLongestMatch(true)
          .minSubwordSize(MIN_SUBWORD_SIZE))))

      .analyzer("default", o -> o
      .custom(i -> i
        .tokenizer("standard")
        .filter("lowercase", "german_partial_word_search")))));
  }

  @Override
  public long getIndexDocumentCount(String index) throws IOException {
    final var countRequest = new CountRequest(index);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    countRequest.source(searchSourceBuilder);

    CountResponse countResponse = highLevelClient.count(countRequest, RequestOptions.DEFAULT);

    return countResponse.getCount();
  }


}
