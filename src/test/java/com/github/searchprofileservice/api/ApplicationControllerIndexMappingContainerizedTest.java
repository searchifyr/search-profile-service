package com.github.searchprofileservice.api;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.searchprofileservice.SearchProfileServiceApplication;
import com.github.searchprofileservice.container.AbstractElasticSearchTestContainer;
import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.support.JsonDocumentSample;
import com.github.searchprofileservice.support.SecurityDummyUser;
import com.github.searchprofileservice.util.IndexHelper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static com.github.searchprofileservice.api.routes.Routes.Api.V1.Applications.GetOne;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = SearchProfileServiceApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationControllerIndexMappingContainerizedTest extends AbstractElasticSearchTestContainer {

  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final static String sampleIndexName = "807622ae-25f5-4dca-b6c8-af262bf03723";

  private final static String getUrl =
    GetOne.GetMapping
      .replace("{" + GetOne.PathParams.applicationId + "}", sampleIndexName);

  @Autowired
  private ElasticsearchClient elasticsearchClient;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AuthenticationService authenticationService;

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void return_correct_mapping_on_application_mapping_call() {

    // arrange
    deleteIndexIfExists(sampleIndexName);
    createIndex(sampleIndexName);
    insertDocumentIntoIndex(sampleIndexName,
        JsonDocumentSample.getElasticSearchIndexJsonDocumentSample());

    Map<String, ElasticSearchMappingType> expected = new TreeMap<>();
    expected.put("name", ElasticSearchMappingType.TEXT);
    expected.put("certificate.name", ElasticSearchMappingType.TEXT);
    expected.put("certificate.available", ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put("object1.object2.object3.object4.object5", ElasticSearchMappingType.NOT_SUPPORTED);

    when(authenticationService.getUser())
        .thenReturn(new AuthenticatedUser(SecurityDummyUser.TEST_USER_NAME, "1", ""));

    // act
    MvcResult mvcResult
        = mockMvc.perform(get(getUrl).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andReturn();

    // assert
    Map<String, ElasticSearchMappingType> result
        = getTreeMapFromJson(mvcResult.getResponse().getContentAsString());

    assertEquals(expected, result);
  }

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void return_not_found_status_if_index_does_not_exist_on_mapping_request() {
    // arrange
    when(authenticationService.getUser())
        .thenReturn(new AuthenticatedUser(SecurityDummyUser.TEST_USER_NAME, "1", ""));

    deleteIndexIfExists(sampleIndexName);

    // act && assert
    mockMvc.perform(get(getUrl).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound()).andReturn();
  }

  private Map<String, ElasticSearchMappingType> getTreeMapFromJson(String json)
      throws JsonProcessingException {

    return OBJECT_MAPPER.readValue(json,
        new TypeReference<TreeMap<String, ElasticSearchMappingType>>() {});
  }

  private void deleteIndexIfExists(String indexName) throws IOException {
    IndexHelper.deleteIndexIfExists(elasticsearchClient, indexName);
  }

  private void createIndex(String indexName) throws IOException {
    IndexHelper.createIndex(elasticsearchClient, indexName);
  }

  private void insertDocumentIntoIndex(String indexName, String json) throws IOException {
    IndexHelper.insertDocumentIntoIndex(elasticsearchClient, indexName, json);
  }

}