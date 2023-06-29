package com.github.searchprofileservice.service;

import com.github.searchprofileservice.persistence.mongo.model.base.ApiKey;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.persistence.mongo.repository.ApplicationRepository;
import com.github.searchprofileservice.service.impl.ApplicationServiceImpl;
import javassist.NotFoundException;
import lombok.SneakyThrows;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ApplicationServiceTest {

  private final ApplicationRepository applicationRepository =
    mock(ApplicationRepository.class);

  private final SearchProfileService searchProfileService =
    mock(SearchProfileService.class);

  private final ElasticSearchClientService elasticSearchService =
          mock(ElasticSearchClientService.class);
  private final AuthenticationService authenticationService =
          mock(AuthenticationService.class);

  private final ArrayList<ApiKey> apiKeys = new ArrayList<ApiKey>();

  @InjectMocks
  private final ApplicationService applicationService =
    new ApplicationServiceImpl(applicationRepository, searchProfileService, elasticSearchService, authenticationService);

  @BeforeEach
  public void SetUp(){
    ApiKey apiKey = new ApiKey(UUID.randomUUID(), "example", UUID.randomUUID().toString());
    apiKeys.add(apiKey);
  }

  @Test
  public void findAll() {
    List<Application> applications =
      List.of(
        new Application(
          UUID.randomUUID(),
          Calendar.getInstance().getTime(),
          apiKeys,
          "foo",
          "foo",
          false,
                List.of("111")),
        new Application(
          UUID.randomUUID(),
          Calendar.getInstance().getTime(),
          apiKeys,
          "bar",
          "bar",
          false,
                List.of("111"))
      );

    when(applicationRepository.findAll())
      .thenReturn(applications);

    assertThat(applicationService.findAll(), equalTo(applications));
    verify(applicationRepository, times(1)).findAll();
  }

  @Test
  public void existsById() {
    final UUID uuid1 = UUID.randomUUID();
    final UUID uuid2 = UUID.randomUUID();

    when(applicationRepository.existsById(any(UUID.class)))
      .thenAnswer(i -> {
        UUID uuid = (UUID) i.getArgument(0);
        return uuid.equals(uuid1) ? true : false;
      });

    assertThat(applicationService.existsById(uuid1), equalTo(true));
    assertThat(applicationService.existsById(uuid2), equalTo(false));
    verify(applicationRepository, times(2)).existsById(any(UUID.class));
  }

  @Test
  public void findById() {
    final UUID uuid1 = UUID.randomUUID();
    final UUID uuid2 = UUID.randomUUID();
    final UUID uuid3 = UUID.randomUUID();

    final Application a1 =
      new Application(
        uuid1,
        Calendar.getInstance().getTime(),
        apiKeys,
        "foo",
        "foo",
        false,
              List.of("111"));

    final Application a2 =
      new Application(
        uuid2,
        Calendar.getInstance().getTime(),
        apiKeys,
        "bar",
        "bar",
        false,
              List.of("111"));

    when(applicationRepository.findById(any(UUID.class)))
      .thenAnswer(i -> {
        UUID uuid = (UUID) i.getArgument(0);

        return uuid.equals(uuid1) ? Optional.of(a1) : uuid.equals(uuid2) ? Optional.of(a2) : Optional.empty();
      });

    assertThat(applicationService.findById(uuid1), equalTo(Optional.of(a1)));
    assertThat(applicationService.findById(uuid2), equalTo(Optional.of(a2)));
    assertThat(applicationService.findById(uuid3), equalTo(Optional.empty()));
    verify(applicationRepository, times(3)).findById(any(UUID.class));
  }

  @Test
  public void save_ApplicationIsRedundantInDB_ReturnsOptionalEmpty() {
    Application a1 =
      new Application(
        null,
        null,
        apiKeys,
        "foo",
        "foo",
        false,
              List.of("111"));

    when(applicationRepository.existsApplicationByApplicationName(any(String.class))).thenAnswer(i -> {
              return Boolean.TRUE;
            });
    var result = applicationService.save(a1);
    assertThat(result, is(Optional.empty()));
  }


  @Test
  public void save_ApplicationIsRedundantInES_ReturnsOptionalEmpty() throws IOException {
    Application a1 =
            new Application(
                    UUID.randomUUID(),
                    Calendar.getInstance().getTime(),
                    apiKeys,
                    "foo",
                    "foo",
                    false,
                    List.of("111"));

    when(applicationRepository.existsApplicationByApplicationName(any(String.class))).thenAnswer(i -> {
      return Boolean.FALSE;
    });


    when(elasticSearchService.createIndex(any(Application.class))).thenAnswer( i -> { return false;});

    var result = applicationService.save(a1);
    assertThat(result, is(Optional.empty()));
    verify(elasticSearchService, times(1)).createIndex(any(Application.class));
  }

  @Test
  public void save_ApplicationIsValid_ReturnsSavedApplication(){
    Application testApplication =
            new Application(
                    UUID.randomUUID(),
                    Calendar.getInstance().getTime(),
                    apiKeys,
                    "foo",
                    "foo",
                    false,
                    List.of("111"));

    when(applicationRepository.existsApplicationByApplicationName(any(String.class))).thenAnswer(i -> {
      return Boolean.FALSE;
    });
    when(applicationRepository.save(testApplication))
            .thenAnswer(i -> {
              return testApplication;
            });

    when(elasticSearchService.createIndex(any(Application.class))).thenAnswer( i -> { return true;});


    var result = applicationService.save(testApplication);
    assertThat(result, is(Optional.of(testApplication)));
    assertThat(result.get().getId(), is(testApplication.getId()));
    assertThat(result.get().getApiKeys(), is(testApplication.getApiKeys()));
    assertThat(result.get().getCreatedDate(), is(testApplication.getCreatedDate()));
    verify(applicationRepository, times(1)).save(testApplication);
  }


  @Test
  public void save_ApplicationInputIsNotValid_ThrowsOnMissingName() {
    Application a1 =
      new Application(
        null,
        null,
        null,
        "foo",
        false,
              List.of("111"));

    assertThrows(
      IllegalArgumentException.class,
      () -> applicationService.save(a1));
    verify(applicationRepository, never()).save(any(Application.class));
  }

  @Test
  public void saveThrowsOnMissingCreatorId() {
    Application a1 =
      new Application(
        null,
        null,
        apiKeys,
        null,
        false,
              List.of("111"));

    assertThrows(
      IllegalArgumentException.class,
      () -> applicationService.save(a1));
    verify(applicationRepository, never()).save(any(Application.class));
  }

  @Test
  @SneakyThrows
  public void uploadDocument_ApplicationNotExistingInDb_Throws_IllegalArgumentException() {

    String documentData = "1";
    var applicationId = UUID.randomUUID();

    when(applicationRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThrows(
      IllegalArgumentException.class,
      () -> applicationService.uploadDocument(documentData, applicationId));
    verify(applicationRepository, times(1)).findById(any(UUID.class));
  }

  @Test
  @SneakyThrows
  public void uploadDocument_uploadRawJsonToApplicationFailed_Throws_IOException() {
    String inputOne = "1";
    var inputTwo = UUID.randomUUID();

    var app = new Application(
            UUID.randomUUID(),
            Calendar.getInstance().getTime(),
            apiKeys,
            "foo",
            "foo",
            true,
            List.of("111"));
    Optional<Application> mockReturn = Optional.ofNullable(app);

    when(applicationRepository.findById(any(UUID.class))).thenReturn(mockReturn);
    doThrow(IOException.class).when(elasticSearchService).uploadRawJsonToApplication(any(UUID.class), any(String.class));

    assertThrows(
      IOException.class,
      () -> applicationService.uploadDocument(inputOne, inputTwo));

    verify(applicationRepository, times(1)).findById(any(UUID.class));
    verify(elasticSearchService, times(1)).uploadRawJsonToApplication(any(UUID.class), any(String.class));
  }

  @Test
  @SneakyThrows
  public void uploadDocument_uploadRawJsonToApplicationSuccess_ReturnsOptionalWithDocumentID() {
    String documentData = "1";
    var applicationId = UUID.randomUUID();
    var documentId = UUID.randomUUID().toString();

    var app = new Application(
            UUID.randomUUID(),
            Calendar.getInstance().getTime(),
            apiKeys,
            "foo",
            "foo",
            true,
            List.of("111"));
    Optional<Application> mockReturn = Optional.of(app);

    when(applicationRepository.findById(any(UUID.class)))
      .thenReturn(mockReturn);
    when(elasticSearchService.uploadRawJsonToApplication(any(UUID.class), any(String.class)))
      .thenReturn(documentId);

    var result = applicationService.uploadDocument(documentData, applicationId);
    verify(applicationRepository, times(1)).findById(any(UUID.class));
    verify(elasticSearchService, times(1)).uploadRawJsonToApplication(any(UUID.class), any(String.class));
    assertThat(result, is(documentId));
  }


  @Test
  @SneakyThrows
  public void uploadDocument_ApplicationActivityIsFalse_ApplicationGetsUpdated() {
    String documentData = "1";
    var applicationId = UUID.randomUUID();
    var documentId = UUID.randomUUID().toString();

    var app = new Application(
            applicationId,
            Calendar.getInstance().getTime(),
            apiKeys,
            "foo",
            "foo",
            false,
            List.of("111"));
    Optional<Application> mockReturn = Optional.of(app);


    when(applicationRepository.findById(any(UUID.class)))
            .thenReturn(mockReturn);

    when(elasticSearchService.uploadRawJsonToApplication(any(UUID.class), any(String.class)))
            .thenReturn(documentId);

    var result = applicationService.uploadDocument(documentData, applicationId);
    verify(applicationRepository, times(1)).findById(app.getId());
    verify(applicationRepository, times(1)).save(app);
    verify(elasticSearchService, times(1)).uploadRawJsonToApplication(app.getId(), documentData);
    assertThat(result, is(documentId));
  }

  @Test
  @SneakyThrows
  public void uploadDocument_ApplicationActivityIsFalse_ApplicationGetsNotUpdated() {
    String documentData = "1";
    var applicationId = UUID.randomUUID();
    var documentId = UUID.randomUUID().toString();

    var app = new Application(
            applicationId,
            Calendar.getInstance().getTime(),
            apiKeys,
            "foo",
            "foo",
            true,
            List.of("111"));
    Optional<Application> mockReturn = Optional.of(app);


    when(applicationRepository.findById(any(UUID.class)))
            .thenReturn(mockReturn);

    when(elasticSearchService.uploadRawJsonToApplication(any(UUID.class), any(String.class)))
            .thenReturn(documentId);

    var result = applicationService.uploadDocument(documentData, applicationId);
    verify(applicationRepository, times(1)).findById(app.getId());
    verify(applicationRepository, times(0)).save(app);
    verify(elasticSearchService, times(1)).uploadRawJsonToApplication(app.getId(), documentData);
    assertThat(result, is(documentId));
  }

  @Test
  @SneakyThrows
  public void bulkUploadDocuments_DocumentsIsNotSatisfyingJsonStandard_Throws_JSONException() {
    String failingJson = "failed";
    var applicationId = UUID.randomUUID();

    assertThrows(
            JSONException.class,
            () -> applicationService.bulkUploadDocuments(failingJson, applicationId));
  }

  @Test
  @SneakyThrows
  public void bulkUploadDocuments_ApplicationNotExistingInDb_Throws_IllegalArgumentException() {
    String jsonDoc = "{\"Hello\": \"World\"}";
    String legalJsonDoc = "{\"Documents\":[ " + jsonDoc +" ]}";
    var applicationId = UUID.randomUUID();

    when(applicationRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
    assertThrows(
            IllegalArgumentException.class,
            () -> applicationService.bulkUploadDocuments(legalJsonDoc, applicationId));
    verify(applicationRepository, times(1)).findById(any(UUID.class));
    verify(elasticSearchService, times(0)).bulkUploadRawJsonToApplication(any(UUID.class), any(ArrayList.class));
  }

  @Test
  @SneakyThrows
  public void bulkUploadDocuments_DocumentsUploadedSuccessful_ReturnDocIds() {
    String jsonDoc = "{\"Hello\": \"World\"}";
    String legalJsonDoc = "{\"Documents\":[ " + jsonDoc +" ]}";
    var applicationId = UUID.randomUUID();

    var app = new Application(
            applicationId,
            Calendar.getInstance().getTime(),
            apiKeys,
            "foo",
            "foo",
            false,
            List.of("111"));
    Optional<Application> mockReturn = Optional.of(app);


    when(applicationRepository.findById(any(UUID.class))).thenReturn(mockReturn);
    when(elasticSearchService.bulkUploadRawJsonToApplication(any(UUID.class), any(ArrayList.class))).thenReturn(Arrays.asList("r@ndomId"));

    applicationService.bulkUploadDocuments(legalJsonDoc, applicationId);
    verify(applicationRepository, times(1)).findById(any(UUID.class));
    verify(elasticSearchService, times(1)).bulkUploadRawJsonToApplication(any(UUID.class), any(ArrayList.class));
  }



  @Test
  @SneakyThrows
  public void updateDocument_ApplicationNotExistingInDb_Throws_IllegalArgumentException() {

    String documentData = "1";
    var applicationId = UUID.randomUUID();
    var documentId = "1";

    when(applicationRepository.existsById(any(UUID.class))).thenReturn(false);

    assertThrows(
      IllegalArgumentException.class,
      () -> applicationService.updateDocument(documentData, applicationId, documentId));

    verify(applicationRepository, times(1)).existsById(any(UUID.class));
    verify(elasticSearchService, never()).updateDocument(any(UUID.class), anyString(), anyString());
  }

  @Test
  @SneakyThrows
  public void updateDocument_Sucess() {

    String documentData = "1";
    var applicationId = UUID.randomUUID();
    var documentId = "1";

    when(applicationRepository.existsById(any(UUID.class))).thenReturn(true);

    assertDoesNotThrow(
      () -> applicationService.updateDocument(documentData, applicationId, documentId));

    verify(applicationRepository, times(1)).existsById(any(UUID.class));
    verify(elasticSearchService, times(1)).updateDocument(applicationId, documentId, documentData);
  }

  @Test
  public void update() {
    final UUID id = UUID.randomUUID();
    final Date createdDate = Calendar.getInstance().getTime();
    Application a1 =
      new Application(
        id,
        createdDate,
        apiKeys,
        "foo",
        "foo",
        false,
              List.of("111"));

    when(applicationRepository.save(any(Application.class)))
      .thenAnswer(i -> i.getArgument(0));

    Application result = applicationService.update(a1);
    assertThat(result.getId(), equalTo(id));
    assertThat(result.getApiKeys(), equalTo(apiKeys));
    assertThat(result.getCreatedDate(), equalTo(createdDate));
    verify(applicationRepository, times(1)).save(any(Application.class));
  }

  @Test
  public void updateThrowsOnMissingName() {
    Application a1 =
      new Application(
        UUID.randomUUID(),
        null,
        null,
        "foo",
        false,
              List.of("111"));

    assertThrows(
      IllegalArgumentException.class,
      () -> applicationService.update(a1));
    verify(applicationRepository, never()).save(any(Application.class));
  }

  @Test
  public void updateThrowsOnMissingCreatorId() {
    Application a1 =
      new Application(
        UUID.randomUUID(),
        null,
        apiKeys,
        "foo",
        null,
        false,
              List.of("111"));

    assertThrows(
      IllegalArgumentException.class,
      () -> applicationService.update(a1));
    verify(applicationRepository, never()).save(any(Application.class));
  }

  @Test
  public void updateThrowsOnMissingID() {
    Application a1 =
      new Application(
        null,
        null,
        apiKeys,
        "foo",
        "foo",
        false,
              List.of("111"));

    assertThrows(
      IllegalArgumentException.class,
      () -> applicationService.update(a1));
    verify(applicationRepository, never()).save(any(Application.class));
  }

  @Test
  public void updateThrowsOnMissingApiKey() {
    Application a1 =
      new Application(
        UUID.randomUUID(),
        null,
        apiKeys,
        "foo",
        false,
              List.of("111"));

    assertThrows(
      IllegalArgumentException.class,
      () -> applicationService.update(a1));
    verify(applicationRepository, never()).save(any(Application.class));
  }

  @Test
  @SneakyThrows
  public void deleteByIdSuccessful() {
    doNothing().when(applicationRepository).deleteById(any(UUID.class));
    doNothing().when(searchProfileService).deleteByApplicationId(any(UUID.class));
    doNothing().when(elasticSearchService).deleteIndex(any(UUID.class));

    final UUID uuid = UUID.randomUUID();

    applicationService.deleteById(uuid);
    verify(applicationRepository, times(1)).deleteById(uuid);
  }

  @Test
  @SneakyThrows
  public void deleteByIdElasticSeachError() {
    doNothing().when(applicationRepository).deleteById(any(UUID.class));
    doNothing().when(searchProfileService).deleteByApplicationId(any(UUID.class));
    doThrow(IOException.class).when(elasticSearchService).deleteIndex(any(UUID.class));

    final UUID uuid = UUID.randomUUID();

    assertThrows(
      IOException.class,
      () -> applicationService.deleteById(uuid));

    verify(applicationRepository, never()).deleteById(uuid);
    verify(searchProfileService, never()).deleteByApplicationId(uuid);
  }

  @Test
  public void JsonIsValid_JsonIsNotValid_ReturnFalse() {
   final String notValidJsonOne = "test";
    final String notValidJsonTwo = "{hello : World}";
    final String notValidJsonThree = "gh\"Hello\" : \"World\"";
    final String notValidJsonFour = "{\n" +
            "    \"glossary\": {\n" +
            "        \"title\": example glossary\",\n" +
            "\t\t\"GlossDiv\": {\n" +
            "            \"title\": \"S\",\n" +
            "\t\t\t\"GlossList\": {\n" +
            "                \"GlossEntry\": {\n" +
            "                    \"ID\": \"SGML\",\n" +
            "\t\t\t\t\t\"SortAs\": \"SGML\",\n" +
            "\t\t\t\t\t\"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
            "\t\t\t\t\t\"Acronym\": \"SGML\",\n" +
            "\t\t\t\t\t\"Abbrev\": \"ISO 8879:1986\",\n" +
            "\t\t\t\t\t\"GlossDef\": {\n" +
            "                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n" +
            "\t\t\t\t\t\t\"GlossSeeAlso\": [\"GML\", \"XML\"]\n" +
            "                    },\n" +
            "\t\t\t\t\t\"GlossSee\": \"markup\"\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";


    boolean resultOne = applicationService.isJsonValid(notValidJsonOne);
    boolean resultTwo = applicationService.isJsonValid(notValidJsonTwo);
    boolean resultThree = applicationService.isJsonValid(notValidJsonThree);
    boolean resultFour = applicationService.isJsonValid(notValidJsonFour);

    assertFalse(resultOne);
    assertFalse(resultTwo);
    assertFalse(resultThree);
    assertFalse(resultFour);
  }

  @Test
  public void JsonIsValid_JsonIsValid_ReturnTrue() {
    final String ValidJsonOne = "{\"name\":\"John\", \"age\":30, \"car\":null}";
    final String ValidJsonTwo = "{\"hello\" : \"World\"}";
    final String ValidJsonThree = "{\n" +
            "  \"squadName\": \"Super hero squad\",\n" +
            "  \"homeTown\": \"Metro City\",\n" +
            "  \"formed\": 2016,\n" +
            "  \"secretBase\": \"Super tower\",\n" +
            "  \"active\": true,\n" +
            "  \"members\": [\n" +
            "    {\n" +
            "      \"name\": \"Molecule Man\",\n" +
            "      \"age\": 29,\n" +
            "      \"secretIdentity\": \"Dan Jukes\",\n" +
            "      \"powers\": [\n" +
            "        \"Radiation resistance\",\n" +
            "        \"Turning tiny\",\n" +
            "        \"Radiation blast\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Madame Uppercut\",\n" +
            "      \"age\": 39,\n" +
            "      \"secretIdentity\": \"Jane Wilson\",\n" +
            "      \"powers\": [\n" +
            "        \"Million tonne punch\",\n" +
            "        \"Damage resistance\",\n" +
            "        \"Superhuman reflexes\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Eternal Flame\",\n" +
            "      \"age\": 1000000,\n" +
            "      \"secretIdentity\": \"Unknown\",\n" +
            "      \"powers\": [\n" +
            "        \"Immortality\",\n" +
            "        \"Heat Immunity\",\n" +
            "        \"Inferno\",\n" +
            "        \"Teleportation\",\n" +
            "        \"Interdimensional travel\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    final String ValidJsonFour = "{\n" +
            "    \"glossary\": {\n" +
            "        \"title\": \"example glossary\",\n" +
            "\t\t\"GlossDiv\": {\n" +
            "            \"title\": \"S\",\n" +
            "\t\t\t\"GlossList\": {\n" +
            "                \"GlossEntry\": {\n" +
            "                    \"ID\": \"SGML\",\n" +
            "\t\t\t\t\t\"SortAs\": \"SGML\",\n" +
            "\t\t\t\t\t\"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
            "\t\t\t\t\t\"Acronym\": \"SGML\",\n" +
            "\t\t\t\t\t\"Abbrev\": \"ISO 8879:1986\",\n" +
            "\t\t\t\t\t\"GlossDef\": {\n" +
            "                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n" +
            "\t\t\t\t\t\t\"GlossSeeAlso\": [\"GML\", \"XML\"]\n" +
            "                    },\n" +
            "\t\t\t\t\t\"GlossSee\": \"markup\"\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";


    boolean resultOne = applicationService.isJsonValid(ValidJsonOne);
    boolean resultTwo = applicationService.isJsonValid(ValidJsonTwo);
    boolean resultThree = applicationService.isJsonValid(ValidJsonThree);
    boolean resultFour = applicationService.isJsonValid(ValidJsonFour);

    assertTrue(resultOne);
    assertTrue(resultTwo);
    assertTrue(resultThree);
    assertTrue(resultFour);
  }

  @Test
  public void addNewApiKeyToApp_happyPath(){
    Application mockApplication = new Application(
            UUID.randomUUID(),
            new Date(),
            new ArrayList<ApiKey>(),
            "name",
            "foo",
            false,
            List.of("111")
    );
    when(applicationRepository.save(any(Application.class))).thenReturn(mockApplication);
    applicationService.addNewApiKeyToApp(mockApplication, new ApiKey(UUID.randomUUID(), "newApiKey", UUID.randomUUID().toString()));

    assertThat(mockApplication.getApiKeys().stream().findFirst().get().getName(), equalTo("newApiKey"));
  }

  @Test
  @SneakyThrows
  public void deleteApiKeyFromApp_happyPath(){
    ArrayList<ApiKey> apiKeys = new ArrayList<>();
    UUID apiKeyId = UUID.fromString("ea252733-81e3-40d7-9dfd-5f4dda10dc49");
    apiKeys.add(new ApiKey(apiKeyId, "apiKeyOne", UUID.randomUUID().toString()));
    Application mockApplication = new Application(UUID.randomUUID(), new Date(), apiKeys, "name", "foo",false, List.of("111"));
    when(applicationRepository.save(any(Application.class))).thenReturn(new Application());

    applicationService.deleteApiKeyFromApp(mockApplication, apiKeyId);

    List<ApiKey> resultApiKeys = mockApplication.getApiKeys();
    assertThat(resultApiKeys, hasSize(0));
  }

  @Test
  @SneakyThrows
  public void deleteApiKeyFromApp_happyPathWithThreeKeys(){
    ArrayList<ApiKey> apiKeys = new ArrayList<>();
    UUID idOfApiKeyToDelete = UUID.fromString("ea252733-81e3-40d7-9dfd-5f4dda10dc49");
    UUID idOfApiKeyToKeep = UUID.fromString("ea252733-81e3-40d7-9dfd-5f4dda10dc49");
    apiKeys.add(new ApiKey(idOfApiKeyToKeep, "apiKeyOne", UUID.randomUUID().toString()));
    apiKeys.add(new ApiKey(idOfApiKeyToDelete, "apiKeyThree", UUID.randomUUID().toString()));
    apiKeys.add(new ApiKey(UUID.randomUUID(), "apiKeyFive", UUID.randomUUID().toString()));
    Application mockApplication = new Application(UUID.randomUUID(), new Date(), apiKeys, "name", "foo",false, List.of("111"));
    when(applicationRepository.save(any(Application.class))).thenReturn(new Application());

    applicationService.deleteApiKeyFromApp(mockApplication, idOfApiKeyToDelete);

    List<ApiKey> resultApiKeys = mockApplication.getApiKeys();
    assertThat(resultApiKeys, hasSize(2));
    assertThat(resultApiKeys.stream().findFirst().get().getId(), equalTo(idOfApiKeyToKeep));
  }

  @Test
  public void deleteApiKeyFromApp_whenCalledWithIdWhichDoesNotExist_ThrowsException(){
    ArrayList<ApiKey> apiKeys = new ArrayList<>();
    apiKeys.add(new ApiKey(UUID.randomUUID(), "apiKeyOne", UUID.randomUUID().toString()));
    apiKeys.add(new ApiKey(UUID.randomUUID(), "apiKeyFive", UUID.randomUUID().toString()));
    Application mockApplication = new Application(UUID.randomUUID(), new Date(), apiKeys, "name", "foo",false,  List.of("111"));
    when(applicationRepository.save(any(Application.class))).thenReturn(new Application());

    assertThrows(NotFoundException.class, () -> applicationService
            .deleteApiKeyFromApp(mockApplication, UUID.randomUUID()));
  }
}
