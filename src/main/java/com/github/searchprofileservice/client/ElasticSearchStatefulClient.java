package com.github.searchprofileservice.client;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.client.security.GetRolesResponse;
import org.elasticsearch.client.security.GetUsersResponse;
import org.elasticsearch.client.security.PutRoleResponse;
import org.elasticsearch.client.security.PutUserResponse;
import org.elasticsearch.client.security.user.privileges.IndicesPrivileges;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ElasticSearchStatefulClient {


  /**
   * Get a Roles from Elasticsearch
   *
   * @return <see>GetRolesResponse
   *@throws IOException
   */
  GetRolesResponse getRole(String roleName) throws IOException;


  /**
   * Create a User Role in ElasticSearch
   * @param roleName Role Name
   * @param clusterPrivilege Cluster Privilege which the Role does have
   * @param indicesPrivileges Indicie Privileges which the role does have
   * @return <see>PutUserResponse
   *
   */
  PutRoleResponse putRole(String roleName, String[] clusterPrivilege, ArrayList<IndicesPrivileges> indicesPrivileges) throws IOException;

  /**
  * Create a User in ElasticSearch Instance
  * @param userName userName from the new User
  * @param password password from the new User as char array
  * @return <see>PutUserResponse
  *
   */
  PutUserResponse createElasticUser(String userName, char[] password) throws IOException;

  /**
   * Get all  created User in Elasticsearch
   *
   * @return <see>GetUsersResponse
   *
   */
  GetUsersResponse getAllUser() throws IOException;

  /**
   * Check if a connection to a running elasticsearch instance can be established.
   *
   * @return Whether a connection can be established.
   */
  boolean isConnectionAvailable() throws IOException;

  /**
   * Creates a Index in Elasticsearch.
   * @param name Name of the index to create.
   * @return Whether the index was successfully created.
   */
  boolean createIndex(String name) throws IOException;

  /**
   * Removes an index from elastic search
   * @param name the name of the index to remove
   * @throws IOException on error while removal
   */
  void deleteIndex(String name) throws IOException;

  /**
   * Uploads Raw json to specific Index
   * @param indexName
   * @param json
   * @return the id of the created document
   */
  String uploadRawJsonToIndex(String indexName, String json) throws IOException;

  /**
   * Updates an existing document
   * @param indexName
   * @param documentId
   * @param json
   */
  void updateDocument(String indexName, String documentId, String json) throws IOException;


  /**
   * Uploads Raw json documents to specific Index
   * @param indexName
   * @param jsonDocuments documents to upload
   * @return the id of the created documents
   */
  List<String> bulkUploadRawJsonToIndex(String indexName, List<String> jsonDocuments) throws IOException;

  /**
   *
   * @param indexName Name of the index to create.
   * @return  The Mapping of the index. The Keys are the top field names and the values are the
   *          corresponding mappings for these fields.
   *
   * @throws IOException
   */
  Map<String, Property> getIndexMapping(String indexName) throws IOException;

  /**
   *
   * @param indexName Name of the index, which will be checked, if existent
   * @return Whether the index is existent
   *
   * @throws IOException
   */
  boolean isIndexExistent(String indexName) throws IOException;

  /**
   *
   * @param request Request, which should be performed with elastic search
   * @return search results of request
   *
   * @throws IOException
   */
  SearchResponse<ObjectNode> search(SearchRequest request) throws IOException;

  /**
   * 
   * @return Index settings for the custom partial word analyzer
   * 
   */
  IndexSettings getCustomPartialWordAnalyzerSettings();

  /**
   *
   * @param index Name of the Index
   * @return the number of Documents uploaded to a specific Index
   */
  long getIndexDocumentCount(String index) throws IOException;

}
