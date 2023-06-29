package com.github.searchprofileservice.api;

import com.github.searchprofileservice.SearchProfileServiceApplication;
import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.api.routes.Routes;
import com.github.searchprofileservice.api.routes.Routes.Api.V1.SearchProfiles.GetOne;
import com.github.searchprofileservice.api.routes.Routes.Api.V1.SearchProfiles.GetOne.PathParams;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.model.SearchField;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.SearchProfileDocument;
import com.github.searchprofileservice.persistence.mongo.repository.SearchProfileRepository;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import com.github.searchprofileservice.support.SecurityDummyUser;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SearchProfileServiceApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SearchProfileControllerGetSingleSearchProfileTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SearchProfileRepository searchProfileRepository;

  @MockBean
  private ElasticSearchClientService elasticSearchClientService;

  @MockBean
  private AuthenticationService authenticationService;

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void get_search_profile_by_id_returns_existing_search_profile() {

    // arrange
    String sampleId = "3a52b9c3-337c-4bfb-9ce6-1cc2efa8a7ec";

    String getUrl = Routes.withParams(GetOne.route, PathParams.profileId, sampleId);

    SearchProfileDocument mock = getSearchProfileDocumentSample(sampleId);
    when(searchProfileRepository.findById(eq(sampleId))).thenReturn(Optional.of(mock));
    
    final Map<String, ElasticSearchMappingType> elasticSearchIndexMappingMock = new HashMap<>() {{
      var mockSearchFields = 
        Optional.ofNullable(mock.getSearchFields()).orElseGet(Collections::emptyList);
      for (var field : mockSearchFields) {
        put(field.getFieldName(), ElasticSearchMappingType.TEXT);
      }
    }};
    
    // mock ES service because loading a SearchProfile triggers a lookup into the corresponding
    // ES index
    when(elasticSearchClientService.getIndexMapping(any(String.class)))
      .thenReturn(elasticSearchIndexMappingMock);
    
    AuthenticatedUser mockUser = new AuthenticatedUser(
        SecurityDummyUser.TEST_USER_NAME, SecurityDummyUser.USER_ID, "");
    when(authenticationService.getUser()).thenReturn(mockUser);

    SearchProfileDto expected = mock.toSearchProfileDto();

    // act
    MvcResult mvcResult = mockMvc.perform(get(getUrl).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andReturn();

    // assert
    assertNotNull(mvcResult);
    assertEquals(expected, objectMapper.readValue(
        mvcResult.getResponse().getContentAsString(), SearchProfileDto.class));
  }

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void get_search_profile_by_id_returns_not_found_on_non_existing_search_profile() {

    // arrange
    String sampleId = "3a52b9c3-337c-4bfb-9ce6-1cc2efa8a7ec";

    when(searchProfileRepository.findById(eq(sampleId))).thenReturn(Optional.empty());

    String getUrl = Routes.withParams(GetOne.route, PathParams.profileId, sampleId);

    AuthenticatedUser mockUser = new AuthenticatedUser(
        SecurityDummyUser.TEST_USER_NAME, SecurityDummyUser.USER_ID, "");
    when(authenticationService.getUser()).thenReturn(mockUser);

    // act && assert
    mockMvc.perform(get(getUrl).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
  }

  private SearchProfileDocument getSearchProfileDocumentSample(String profileId) {
   return SearchProfileDocument.builder()
       .profileId(profileId)
       .applicationId("ef02e5ee-751b-4f4f-bc1b-01166419b67d")
       .creatorId("1657649955444")
       .lastEditorId("1657649955444")
       .name("SearchprofileName")
       .analyser(new Analyser())
       .searchFields(Collections.singletonList(
           new SearchField("name", true, 1.0)))
       .build();
  }
}
