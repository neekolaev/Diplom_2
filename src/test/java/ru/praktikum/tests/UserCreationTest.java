package ru.praktikum.tests;

import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.praktikum.client.UserClient;
import ru.praktikum.model.User;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.apache.http.HttpStatus.*;

public class UserCreationTest {

    private UserClient userClient;
    private User user;
    private String accessToken;

    @Before
    public void setUp() {
        userClient = new UserClient();
        // Генерируем уникальные данные для каждого теста
        user = new User(
                "test_user_" + System.currentTimeMillis() + "@example.com",
                "password123",
                "TestUser"
        );
    }

    @After
    public void tearDown() {
        // Удаляем пользователя, если токен был получен
        if (accessToken != null && !accessToken.isEmpty()) {
            userClient.delete(accessToken);
        }
    }

    @Test
    @DisplayName("Создание уникального пользователя")
    public void userCanBeCreatedWithValidData() {
        ValidatableResponse response = userClient.create(user);

        int statusCode = response.extract().statusCode();
        assertThat("Status code is not 200", statusCode, equalTo(SC_OK));

        boolean isUserCreated = response.extract().path("success");
        assertThat("User creation failed", isUserCreated, is(true));

        // Сохраняем токен для удаления пользователя в @After
        accessToken = response.extract().path("accessToken");
    }

    @Test
    @DisplayName("Создание пользователя, который уже зарегистрирован")
    public void userCannotBeCreatedTwice() {
        // Сначала создаем пользователя
        ValidatableResponse createResponse = userClient.create(user);
        accessToken = createResponse.extract().path("accessToken"); // Сохраняем токен для удаления

        // Пытаемся создать его еще раз
        ValidatableResponse duplicateResponse = userClient.create(user);

        int statusCode = duplicateResponse.extract().statusCode();
        assertThat("Status code is not 403", statusCode, equalTo(SC_FORBIDDEN));

        boolean isSuccess = duplicateResponse.extract().path("success");
        assertThat("Success is not false", isSuccess, is(false));

        String message = duplicateResponse.extract().path("message");
        assertThat("Message is incorrect", message, equalTo("User already exists"));
    }

    @Test
    @DisplayName("Создание пользователя без обязательного поля (email)")
    public void userCannotBeCreatedWithoutEmail() {
        user.setEmail(null); // Не заполняем поле email
        ValidatableResponse response = userClient.create(user);

        int statusCode = response.extract().statusCode();
        assertThat("Status code is not 403", statusCode, equalTo(SC_FORBIDDEN));

        boolean isSuccess = response.extract().path("success");
        assertThat("Success is not false", isSuccess, is(false));

        String message = response.extract().path("message");
        assertThat("Message is incorrect", message, equalTo("Email, password and name are required fields"));
    }
}