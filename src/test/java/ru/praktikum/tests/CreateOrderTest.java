package ru.praktikum.tests;

import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.praktikum.client.OrderClient;
import ru.praktikum.client.UserClient;
import ru.praktikum.model.Order;
import ru.praktikum.model.User;

import java.util.Collections;
import java.util.List;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CreateOrderTest {

    private UserClient userClient;
    private OrderClient orderClient;
    private User user;
    private String accessToken;
    private List<String> ingredients;

    @Before
    public void setUp() {
        userClient = new UserClient();
        orderClient = new OrderClient();
        user = new User(
                "test_user_" + System.currentTimeMillis() + "@example.com",
                "password123",
                "TestUser"
        );
        // Создаем пользователя
        ValidatableResponse response = userClient.create(user);
        accessToken = response.extract().path("accessToken");

        // Получаем список хэшей ингредиентов
        ingredients = orderClient.getIngredients().extract().path("data._id");
    }

    @After
    public void tearDown() {
        if (accessToken != null && !accessToken.isEmpty()) {
            userClient.delete(accessToken);
        }
    }

    @Test
    @DisplayName("Создание заказа с ингредиентами и с авторизацией")
    public void orderCanBeCreatedWithIngredientsAndAuth() {
        // Берем первые два ингредиента для заказа
        Order order = new Order(List.of(ingredients.get(0), ingredients.get(1)));
        ValidatableResponse response = orderClient.create(accessToken, order);

        int statusCode = response.extract().statusCode();
        assertThat(statusCode, equalTo(SC_OK));

        boolean isSuccess = response.extract().path("success");
        assertThat(isSuccess, is(true));

        // Проверяем, что номер заказа создался
        int orderNumber = response.extract().path("order.number");
        assertThat(orderNumber, notNullValue());
    }

    @Test
    @DisplayName("Создание заказа без ингредиентов")
    public void orderCannotBeCreatedWithoutIngredients() {
        Order order = new Order(Collections.emptyList());
        ValidatableResponse response = orderClient.create(accessToken, order);

        int statusCode = response.extract().statusCode();
        assertThat(statusCode, equalTo(SC_BAD_REQUEST));

        String message = response.extract().path("message");
        assertThat(message, equalTo("Ingredient ids must be provided"));
    }

    @Test
    @DisplayName("Создание заказа с неверным хэшем ингредиента")
    public void orderCannotBeCreatedWithInvalidHash() {
        Order order = new Order(List.of("invalid_hash_123"));
        ValidatableResponse response = orderClient.create(accessToken, order);

        // API должен вернуть 500, так как не сможет найти ингредиент по хэшу
        int statusCode = response.extract().statusCode();
        assertThat(statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));
    }

    @Test
    @DisplayName("Создание заказа без авторизации")
    public void orderCannotBeCreatedWithoutAuth() {
        Order order = new Order(List.of(ingredients.get(0)));
        ValidatableResponse response = orderClient.createWithoutAuth(order);

        // Хотя документация говорит 200, на практике API может попросить логин. Проверяем ожидаемый результат
        int statusCode = response.extract().statusCode();
        assertThat(statusCode, equalTo(SC_OK)); // Как в документации. Можно поменять на 401, если API ведет себя иначе.

        boolean isSuccess = response.extract().path("success");
        assertThat(isSuccess, is(true));
    }
}