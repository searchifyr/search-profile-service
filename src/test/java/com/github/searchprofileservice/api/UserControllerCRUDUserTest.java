
package com.github.searchprofileservice.api;

import com.github.searchprofileservice.api.model.UserDTO;
import com.github.searchprofileservice.model.enums.Role;
import com.github.searchprofileservice.service.AuthenticationService;
import com.github.searchprofileservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class UserControllerCRUDUserTest {
    
    private final UserService userService = mock(UserService.class);
    private final AuthenticationService authenticationService = mock(AuthenticationService.class);

    @InjectMocks
    private final UserController userController = new UserController(userService, authenticationService);

    @Test
    public void getUser_not_found() {
        String userId = "userId";

        ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> userController.getUser(userId));

        assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void updateUser_not_found() {
        String userId = "ThisIsAUserId";
        UserDTO userDTO = new UserDTO(userId, Calendar.getInstance().getTime(), "userName", "thisIsALink", true, Role.USER);

        ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> userController.updateUser(userId, userDTO));

        assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void deleteUser_not_found() {
        String userId = "randomUserId";

        ResponseStatusException responseStatusException =
        assertThrows(ResponseStatusException.class,
            () -> userController.deleteUser(userId));

        assertEquals(responseStatusException.getStatus(), HttpStatus.NOT_FOUND);
    }
}
