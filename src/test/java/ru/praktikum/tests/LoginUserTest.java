package ru.praktikum.tests;

import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.praktikum.client.UserClient;
import ru.praktikum.model.User;
import ru.praktikum.model.UserCredentials;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class LoginUserTest {

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
        // Создаем пользователя перед каждым тестом
        ValidatableResponse response = userClient.create(user);
        accessToken = response.extract().path("accessToken");
    }

    @After
    public void tearDown() {
        // Удаляем пользователя после каждого теста
        if (accessToken != null && !accessToken.isEmpty()) {
            userClient.delete(accessToken);
        }
    }

    @Test
    @DisplayName("Логин под существующим пользователем")
    public void userCanLoginWithValidCredentials() {
        UserCredentials credentials = new UserCredentials(user.getEmail(), user.getPassword());
        ValidatableResponse loginResponse = userClient.login(credentials);

        int statusCode = loginResponse.extract().statusCode();
        assertThat("Status code is not 200", statusCode, equalTo(SC_OK));

        boolean isSuccess = loginResponse.extract().path("success");
        assertThat("Login failed", isSuccess, is(true));

        // Проверяем, что в ответе есть токен и он не пустой
        String tokenFromLogin = loginResponse.extract().path("accessToken");
        assertThat("Access token is null or empty", tokenFromLogin, notNullValue());
    }

    @Test
    @DisplayName("Логин с неверным паролем")
    public void userCannotLoginWithWrongPassword() {
        UserCredentials credentials = new UserCredentials(user.getEmail(), "wrong_password");
        ValidatableResponse loginResponse = userClient.login(credentials);

        int statusCode = loginResponse.extract().statusCode();
        assertThat("Status code is not 401", statusCode, equalTo(SC_UNAUTHORIZED));

        boolean isSuccess = loginResponse.extract().path("success");
        assertThat("Success is not false", isSuccess, is(false));
    }
}