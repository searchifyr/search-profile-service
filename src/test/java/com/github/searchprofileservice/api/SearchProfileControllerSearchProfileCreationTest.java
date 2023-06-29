package com.github.searchprofileservice.api;

import com.github.searchprofileservice.SearchProfileServiceApplication;
import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.api.routes.Routes.Api.V1.SearchProfiles;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.model.SearchField;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.persistence.mongo.model.SearchProfileDocument;
import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import com.github.searchprofileservice.persistence.mongo.repository.ApplicationRepository;
import com.github.searchprofileservice.persistence.mongo.repository.SearchProfileRepository;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import com.github.searchprofileservice.service.UserService;
import com.github.searchprofileservice.support.SearchProfileUtil;
import com.github.searchprofileservice.support.SecurityDummyUser;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SearchProfileServiceApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SearchProfileControllerSearchProfileCreationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ArrayList<ApiKey> apiKeys = new ArrayList<ApiKey>();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ApplicationRepository applicationRepository;

  @MockBean
  private SearchProfileRepository searchProfileRepository;

  @MockBean
  private AuthenticationService authenticationService;

  @MockBean
  private UserService userService;

  @MockBean
  private ElasticSearchClientService elasticSearchClientService;

  @BeforeEach
  public void SetUp(){
    ApiKey apiKey = new ApiKey(UUID.randomUUID(), "example", "e007634f-c37d-4ca5-8934-2b74858b40e4");
    apiKeys.add(apiKey);
  }

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void search_profile_creation_on_active_and_existent_application_creates_search_profile() {

    // arrange
    SearchProfileDto body = SearchProfileUtil.getSearchProfileDtoWithAllParams();

    String profileId = "fd596fd6-71b2-4f05-b796-971d6388616f";

    List<SearchField> searchFields = new ArrayList<>();
    searchFields.add(new SearchField("field", true, 1.0));

    SearchProfileDto expected = SearchProfileUtil.getSearchProfileDtoWithAllParams();
    expected.setSearchFields(searchFields);
    expected.setProfileId(profileId);
    expected.setCreatorId(SecurityDummyUser.USER_ID);
    expected.setLastEditorId(SecurityDummyUser.USER_ID);

    Optional<Application> applicationMock = Optional.of(new Application(
        body.getApplicationId(),
        new Date(1657389058158L),
        apiKeys,
        "Application",
        SecurityDummyUser.USER_ID,
        true,
            List.of("111")));

    SearchProfileDocument searchProfileDocumentMock = SearchProfileDocument.of(body);
    searchProfileDocumentMock.setProfileId(profileId);
    searchProfileDocumentMock.setCreatorId(SecurityDummyUser.USER_ID);
    searchProfileDocumentMock.setLastEditorId(SecurityDummyUser.USER_ID);
    searchProfileDocumentMock.setSearchFields(searchFields);
    searchProfileDocumentMock.setAnalyser(new Analyser());

    AuthenticatedUser userMock
        = new AuthenticatedUser(SecurityDummyUser.TEST_USER_NAME, SecurityDummyUser.USER_ID, "");

    when(authenticationService.getUser()).thenReturn(userMock);

    when(applicationRepository.findById(eq(body.getApplicationId()))).thenReturn(applicationMock);

    when(elasticSearchClientService.getIndexMapping(eq(body.getApplicationId().toString())))
        .thenReturn(Map.of("field", ElasticSearchMappingType.TEXT));

    when(searchProfileRepository.save(any())).thenReturn(searchProfileDocumentMock);

    // act
    MvcResult mvcResult
        = mockMvc.perform(post(SearchProfiles.Post)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated()).andReturn();

    // assert
    assertNotNull(mvcResult);
    assertEquals(expected,
        objectMapper.readValue(mvcResult.getResponse().getContentAsString(), SearchProfileDto.class));
  }

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void search_profile_creation_returns_bad_request_on_empty_application_id() {

    // arrange
    SearchProfileDto body = SearchProfileUtil.getSearchProfileDtoWithAllParams();
    body.setApplicationId(null);

    // act && assert
    mockMvc.perform(post(SearchProfiles.Post)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void search_profile_creation_returns_bad_request_on_empty_search_profile_name() {

    // arrange
    SearchProfileDto body = SearchProfileUtil.getSearchProfileDtoWithAllParams();
    body.setName(null);

    // act && assert
    mockMvc.perform(post(SearchProfiles.Post)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void search_profile_creation_returns_bad_request_on_non_existent_application() {

    // arrange
    SearchProfileDto body = SearchProfileUtil.getSearchProfileDtoWithAllParams();

    when(applicationRepository.findById(eq(body.getApplicationId()))).thenReturn(Optional.empty());

    // act && assert
    mockMvc.perform(post(SearchProfiles.Post)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
  @SneakyThrows
  void search_profile_creation_returns_bad_request_on_inactive_application() {

    // arrange
    SearchProfileDto body = SearchProfileUtil.getSearchProfileDtoWithAllParams();

    Optional<Application> applicationMock = Optional.of(new Application(
        body.getApplicationId(),
        new Date(1657389058158L),
        apiKeys,
        "Application",
        SecurityDummyUser.USER_ID,
        false,
            List.of("111")));

    when(applicationRepository.findById(eq(body.getApplicationId()))).thenReturn(applicationMock);

    // act && assert
    mockMvc.perform(post(SearchProfiles.Post)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}
