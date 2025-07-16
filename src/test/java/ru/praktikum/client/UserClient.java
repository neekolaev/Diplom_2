package ru.praktikum.client;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import ru.praktikum.model.User;
// ВОТ ЭТОТ ИМПОРТ РЕШАЕТ ПРОБЛЕМУ
import ru.praktikum.model.UserCredentials;

import static io.restassured.RestAssured.given;

public class UserClient extends BaseClient {

    private static final String AUTH_PATH = "auth/";

    @Step("Создание пользователя")
    public ValidatableResponse create(User user) {
        return given()
                .spec(getSpec())
                .body(user)
                .when()
                .post(AUTH_PATH + "register")
                .then();
    }

    @Step("Логин пользователя")
    public ValidatableResponse login(UserCredentials credentials) {
        return given()
                .spec(getSpec())
                .body(credentials)
                .when()
                .post(AUTH_PATH + "login")
                .then();
    }

    @Step("Изменение данных пользователя")
    public ValidatableResponse update(String accessToken, User user) {
        return given()
                .spec(getSpec())
                .header("Authorization", accessToken)
                .body(user)
                .when()
                .patch(AUTH_PATH + "user")
                .then();
    }

    @Step("Удаление пользователя")
    public void delete(String accessToken) {
        // Токен нужно передавать с префиксом, но API Stellar Burgers прощает его отсутствие
        // Для надежности можно добавить "Bearer "
        if (accessToken != null && !accessToken.isEmpty()) {
            given()
                    .spec(getSpec())
                    .header("Authorization", accessToken)
                    .when()
                    .delete(AUTH_PATH + "user")
                    .then();
        }
    }
}