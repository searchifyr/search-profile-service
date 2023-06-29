package com.github.searchprofileservice.api;

import static com.github.searchprofileservice.api.routes.Routes.Api.V1.Users.Delete;
import static com.github.searchprofileservice.api.routes.Routes.Api.V1.Users.GetAll;
import static com.github.searchprofileservice.api.routes.Routes.Api.V1.Users.GetOne;
import static com.github.searchprofileservice.api.routes.Routes.Api.V1.Users.Put;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.github.searchprofileservice.api.routes.Routes.Api.V1.Users.Login;
import com.github.searchprofileservice.api.routes.Routes;
import com.github.searchprofileservice.api.model.ElasticSearchUser;
import com.github.searchprofileservice.api.model.UserDTO;
import com.github.searchprofileservice.api.model.UserLoginStatus;
import com.github.searchprofileservice.model.AuthenticatedUser;
import com.github.searchprofileservice.model.enums.Role;
import com.github.searchprofileservice.persistence.mongo.model.User;
import com.github.searchprofileservice.persistence.mongo.repository.UserRepository;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import com.github.searchprofileservice.service.UserService;
import com.github.searchprofileservice.support.SecurityDummyUser;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;

import static com.github.searchprofileservice.api.routes.Routes.Api.V1.Users.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    private static final String SAMPLE_USER_ID = "1478572c-9a53-473c-a391-6be626cfd762";

    private static final String SAMPLE_USER_NAME = "Adam";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

   // @SpyBean // use SpyBean so methods are *not mocked* by default
    private final AuthenticationService authenticationService = mock(AuthenticationService.class);

    private final UserService userService = mock(UserService.class);

    @InjectMocks
    private final UserController userController = new UserController(userService, authenticationService);

    @MockBean
    private ElasticSearchClientService clientService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN", "USER" })
    void find_all_users_returns_found_users() {
        List<User> mockList = List.of(
                new User(SAMPLE_USER_ID, SAMPLE_USER_NAME, "", false),
                new User("e94b7882-ea0f-4f40-98be-9fcc161793b5", "Bob", "", false)
        );

        when(userRepository.findAll()).thenReturn(mockList);

        MvcResult result = mockMvc.perform(
            get(GetAll)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<UserDTO> resultList
                = objectMapper.readValue(result.getResponse().getContentAsString(),new TypeReference<List<UserDTO>>(){});

        assertNotNull(resultList);
        assertFalse(resultList.isEmpty());
        var mockListAsUserDTOList = mockList.stream().map(x -> UserDTO.fromUser(x)).toList();
        assertEquals(mockListAsUserDTOList,resultList);
    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
    void find_all_users_returns_found_users_if_not_admin() {
        Set<User> mockUsers = Set.of(
                new User(SAMPLE_USER_ID, SAMPLE_USER_NAME, "", true),
                new User("e94b7882-ea0f-4f40-98be-9fcc161793b5", "Bob", "", true)
        );

        when(userRepository.findAllAsBasicProjection()).thenReturn(mockUsers.stream());

        MvcResult result = mockMvc.perform(
            get(GetAll)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        final List<UserDTO.BasicProjection> resultList =
            objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<UserDTO.BasicProjection>>() { });
        final Set<UserDTO.BasicProjection> results =
            resultList.stream().collect(Collectors.toSet());


        assertNotNull(results);
        assertEquals(results.size(), 2);
        Set<UserDTO.BasicProjection> mockUserProjections =
            mockUsers.stream().map(UserDTO.BasicProjection::fromUser).collect(Collectors.toSet());
        assertEquals(mockUserProjections, results);
    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "USER" })
    void find_all_users_returns_found_users_reduced_if_not_admin() {
        Set<User> mockUsers = Set.of(
                new User(SAMPLE_USER_ID, SAMPLE_USER_NAME, "", true),
                new User("e94b7882-ea0f-4f40-98be-9fcc161793b5", "Bob", "", true)
        );

        when(userRepository.findAllAsBasicProjection()).thenReturn(mockUsers.stream());

        MvcResult result = mockMvc.perform(
                get(GetAll)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        final List<UserDTO> resultList =
            objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<UserDTO>>() { });

        assertNotNull(resultList);
        assertEquals(resultList.size(), 2);
        for (UserDTO user : resultList) {
            assertNull(user.getCreatedDate());
            assertNull(user.getPictureLink());
            assertNull(user.getRole());
            assertFalse(user.isActivated());
        }
    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void find_user_by_user_id_returns_found_user() {
        User mockUser = new User(SAMPLE_USER_ID,SAMPLE_USER_NAME,"", false);
        String getUrl =
            Routes.withParams(GetOne.route, GetOne.PathParams.userId, SAMPLE_USER_ID);
        UserDTO expected = new UserDTO(SAMPLE_USER_ID, null, SAMPLE_USER_NAME, "", false, Role.USER);
        when(userRepository.findByUserId(eq(SAMPLE_USER_ID))).thenReturn(mockUser);

        MvcResult result = mockMvc.perform(
                get(getUrl)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()).andReturn();

        UserDTO userDTOResult = getUserDTOFromMvcResult(result);


        assertNotNull(userDTOResult);
        assertEquals(expected, userDTOResult);
    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void http_status_not_found_if_no_user_in_db_for_id() {
        String getUrl =
            Routes.withParams(GetOne.route, GetOne.PathParams.userId, SAMPLE_USER_ID);

        when(userRepository.findByUserId(eq(SAMPLE_USER_ID))).thenReturn(null);

        mockMvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void update_user_by_user_id_updates_user_if_existent() {
        String newUserName = "Bob";
        User mockUser = new User(SAMPLE_USER_ID,null, "", false);
        User updatedMockUser = new User(SAMPLE_USER_ID,newUserName, "", false);

        String putUrl =
            Routes.withParams(Put.route, Put.PathParams.userId, SAMPLE_USER_ID);

        UserDTO requestUser = new UserDTO(null, null, newUserName, "", false, Role.USER);
        UserDTO expected = new UserDTO(SAMPLE_USER_ID, null, newUserName, "", false, Role.USER);

        when(userRepository.findByUserId(eq(SAMPLE_USER_ID))).thenReturn(mockUser);
        when(userRepository.save(eq(updatedMockUser))).thenReturn(updatedMockUser);

        MvcResult result = mockMvc.perform(
            put(putUrl).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestUser)))
                .andExpect(status().isOk()).andReturn();

        UserDTO userDTOResult = getUserDTOFromMvcResult(result);

        assertNotNull(userDTOResult);
        assertEquals(expected,userDTOResult);
    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void http_status_bad_request_on_non_existent_user_name_on_update() {
        User user = new User(null, null, "", false);

        String putUrl =
            Routes.withParams(Put.route, Put.PathParams.userId, SAMPLE_USER_ID);

        mockMvc.perform(
            put(putUrl).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void http_status_not_found_if_user_is_non_existent_on_update() {
        User user = new User(null,  SAMPLE_USER_NAME, "", false);

        String putUrl =
            Routes.withParams(Put.route, Put.PathParams.userId, SAMPLE_USER_ID);

        when(userRepository.findByUserId(eq(SAMPLE_USER_ID))).thenReturn(null);

        mockMvc.perform(
            put(putUrl).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void delete_user_by_user_id_if_existent() {

        String deleteUrl =
            Routes.withParams(Delete.route, Delete.PathParams.userId, SAMPLE_USER_ID);

        when(userRepository.existsById(eq(SAMPLE_USER_ID))).thenReturn(true);
        doNothing().when(userRepository).deleteById(eq(SAMPLE_USER_ID));

        mockMvc.perform(
            delete(deleteUrl).with(csrf()))
                .andExpect(status().isNoContent());

    }

    @SneakyThrows
    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void http_status_not_found_user_by_user_id_on_delete() {

        String deleteUrl =
            Routes.withParams(Delete.route, Delete.PathParams.userId, SAMPLE_USER_ID);

        when(userRepository.existsById(eq(SAMPLE_USER_ID))).thenReturn(false);

        mockMvc.perform(
            delete(deleteUrl).with(csrf()))
                .andExpect(status().isNotFound());

    }

    @Test
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    @SneakyThrows
    void login_status_returns_authenticated_true_on_logged_in_user() {

        // arrange
        UserLoginStatus expected = new UserLoginStatus(true, "1", "user", "123", false, true);
        when(authenticationService.getUser())
            .thenReturn(new AuthenticatedUser("user", "1", "123"));
        when(authenticationService.isUserAuthenticated())
            .thenReturn(true);
        User activeUser = new User("1", new Date(), "user", "123", true, Role.USER);
        doReturn(Optional.of(activeUser)).when(userService).findByUserId(any(String.class));

        // assert
        ResponseEntity<UserLoginStatus> result = userController.isUserLoggedIn();

        assertNotNull(result);
        assertEquals(result.getBody(), expected);
    }

    @Test
    @SneakyThrows
    void login_status_returns_active_false_on_logged_in_but_not_activated_user() {

        // arrange
        var userServiceMock = mock(UserService.class);
        UserLoginStatus expected = new UserLoginStatus(true, "1", "user", "123", false, false);
        when(authenticationService.getUser())
                .thenReturn(new AuthenticatedUser("user", "1", "123"));
        when(authenticationService.isUserAuthenticated())
                .thenReturn(true);
        User activeUser = new User("1", "user", "", false);
        when(userServiceMock.findByUserId(any(String.class)))
                .thenReturn(
                        Optional.of(activeUser)
                );

        // assert
        ResponseEntity<UserLoginStatus> response = userController.isUserLoggedIn();

        assertNotNull(response);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertEquals(response.getBody(), expected);
    }

    @Test
    @SneakyThrows
    void login_status_returns_authenticated_false_on_not_logged_in_user() {

        // arrange
        UserLoginStatus expected = UserLoginStatus.notAuthenticated();

        // act
        MvcResult mvcResult =
            mockMvc.perform(get(Login.Status).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();

        // assert
        UserLoginStatus result = getUserLoginStatusFromMvcResult(mvcResult);

        assertNotNull(result);
        assertEquals(result, expected);
    }

    @SneakyThrows
    private UserLoginStatus getUserLoginStatusFromMvcResult(MvcResult mvcResult) {
        return objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(), UserLoginStatus.class);
    }

    @SneakyThrows
    private UserDTO getUserDTOFromMvcResult(MvcResult mvcResult) {
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDTO.class);
    }

    @Test
    @SneakyThrows
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void createElasticSearchUser_UserNameIsInvalid_ThrowsException(){
        ElasticSearchUser newUser = new ElasticSearchUser("", "P@ssw0rd21".toCharArray());

        mockMvc.perform(
                        post(Create)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void createElasticSearchUser_passwordIsNull_ThrowsException(){
        ElasticSearchUser newUser = new ElasticSearchUser("user", null);
        mockMvc.perform(
                        post(Create)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void createElasticSearchUser_passwordIsNotValid_ThrowsException(){
        ElasticSearchUser newUser = new ElasticSearchUser("user", "invalidPassword".toCharArray());
        mockMvc.perform(
                        post(Create)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void createElasticSearchUser_CreationFailed_ThrowsException(){
        ElasticSearchUser newUser = new ElasticSearchUser("user", "P@ssw0rd21".toCharArray());

        when(clientService.createElasticSearchUser(any(ElasticSearchUser.class))).thenReturn(false);
        mockMvc.perform(
                        post(Create)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    @WithMockUser(value = SecurityDummyUser.TEST_USER_NAME, roles = { "ADMIN" })
    void createElasticSearchUser_CreationSuccessful_ReturnsElasticSearchUser(){
        ElasticSearchUser newUser = new ElasticSearchUser("user", "P@ssw0rd21".toCharArray());

        when(clientService.createElasticSearchUser(any(ElasticSearchUser.class))).thenReturn(true);
                mockMvc.perform(
                                post(Create)
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(newUser)))
                        .andExpect(status().isCreated()).andReturn();
    }

    @Test
    @SneakyThrows
    void activateUser_returns_OK_on_activating_User(){
        HttpStatus expected = HttpStatus.OK;
        User user = new User (SAMPLE_USER_ID, null, SAMPLE_USER_NAME, "", false, Role.USER);

        when(userService.findByUserId(eq(SAMPLE_USER_ID))).thenReturn(Optional.of(user));

        ResponseEntity<UserDTO> response = userController.activateUser(SAMPLE_USER_ID, true);

        assertNotNull(response);
        assertEquals(response.getStatusCode(), expected);
        assertTrue(response.getBody().isActivated());
    }

    @Test
    @SneakyThrows
    void activateUser_returns_OK_on_deactivating_User() {
        HttpStatus expected = HttpStatus.OK;
        User user = new User (SAMPLE_USER_ID, null, SAMPLE_USER_NAME, "", true, Role.USER);

        when(userService.findByUserId(eq(SAMPLE_USER_ID))).thenReturn(Optional.of(user));

        ResponseEntity<UserDTO> response = userController.activateUser(SAMPLE_USER_ID, false);

        assertNotNull(response);
        assertEquals(response.getStatusCode(), expected);
        assertFalse(response.getBody().isActivated());
    }

    @Test
    @SneakyThrows
    void activateUser_returns_forbidden_on_deactivating_Admin() {
        HttpStatus expected = HttpStatus.FORBIDDEN;
        User user = new User (SAMPLE_USER_ID, null, SAMPLE_USER_NAME, "", true, Role.ADMIN);

        when(userService.findByUserId(eq(SAMPLE_USER_ID))).thenReturn(Optional.of(user));

        ResponseStatusException response =
                assertThrows(ResponseStatusException.class,
                        () -> userController.activateUser(SAMPLE_USER_ID, false));

        assertNotNull(response);
        assertEquals(response.getStatus(), expected);
    }

    @Test
    @SneakyThrows
    void activateUser_returns_not_found_on_non_existing_user() {
        HttpStatus expected = HttpStatus.NOT_FOUND;

        when(userService.findByUserId(eq(SAMPLE_USER_ID))).thenReturn(null);

        ResponseStatusException response =
                assertThrows(ResponseStatusException.class,
                        () -> userController.activateUser("123", false));

        assertNotNull(response);
        assertEquals(response.getStatus(), expected);
    }

}
