package ru.praktikum.tests;

import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.praktikum.client.UserClient;
import ru.praktikum.model.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class UpdateUserTest {

    private UserClient userClient;
    private User user;
    private String accessToken;

    @Before
    public void setUp() {
        userClient = new UserClient();
        user = new User(
                "test_user_" + System.currentTimeMillis() + "@example.com",
                "password123",
                "TestUser"
        );
        ValidatableResponse response = userClient.create(user);
        accessToken = response.extract().path("accessToken");
    }

    @After
    public void tearDown() {
        if (accessToken != null && !accessToken.isEmpty()) {
            userClient.delete(accessToken);
        }
    }

    @Test
    @DisplayName("Изменение имени пользователя с авторизацией")
    public void userNameCanBeUpdatedWithAuth() {
        String newName = "NewName";
        User updatedUserData = new User(user.getEmail(), user.getPassword(), newName);

        ValidatableResponse updateResponse = userClient.update(accessToken, updatedUserData);

        int statusCode = updateResponse.extract().statusCode();
        assertThat("Status code is not 200", statusCode, equalTo(SC_OK));

        boolean isSuccess = updateResponse.extract().path("success");
        assertThat("Update failed", isSuccess, is(true));

        String actualName = updateResponse.extract().path("user.name");
        assertThat("User name was not updated", actualName, equalTo(newName));
    }

    @Test
    @DisplayName("Изменение email пользователя с авторизацией")
    public void userEmailCanBeUpdatedWithAuth() {
        String newEmail = "new_" + user.getEmail();
        User updatedUserData = new User(newEmail, user.getPassword(), user.getName());

        ValidatableResponse updateResponse = userClient.update(accessToken, updatedUserData);
        int statusCode = updateResponse.extract().statusCode();
        assertThat(statusCode, equalTo(SC_OK));

        String actualEmail = updateResponse.extract().path("user.email");
        assertThat(actualEmail, equalTo(newEmail.toLowerCase())); // API возвращает email в нижнем регистре
    }

    @Test
    @DisplayName("Изменение данных пользователя без авторизации")
    public void userDataCannotBeUpdatedWithoutAuth() {
        User updatedUserData = new User(user.getEmail(), user.getPassword(), "NewName");
        // Передаем пустой токен
        ValidatableResponse updateResponse = userClient.update("", updatedUserData);

        int statusCode = updateResponse.extract().statusCode();
        assertThat("Status code is not 401", statusCode, equalTo(SC_UNAUTHORIZED));

        boolean isSuccess = updateResponse.extract().path("success");
        assertThat("Success is not false", isSuccess, is(false));

        String message = updateResponse.extract().path("message");
        assertThat("Message is incorrect", message, equalTo("You should be authorised"));
    }
}