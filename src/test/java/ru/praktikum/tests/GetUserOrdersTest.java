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

import java.util.List;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class GetUserOrdersTest {

    private UserClient userClient;
    private OrderClient orderClient;
    private User user;
    private String accessToken;

    @Before
    public void setUp() {
        userClient = new UserClient();
        orderClient = new OrderClient();
        user = new User(
                "test_user_" + System.currentTimeMillis() + "@example.com",
                "password123",
                "TestUser"
        );
        ValidatableResponse response = userClient.create(user);
        accessToken = response.extract().path("accessToken");

        // Создаем один заказ, чтобы было что получать
        List<String> ingredients = orderClient.getIngredients().extract().path("data._id");
        Order order = new Order(List.of(ingredients.get(0)));
        orderClient.create(accessToken, order);
    }

    @After
    public void tearDown() {
        if (accessToken != null && !accessToken.isEmpty()) {
            userClient.delete(accessToken);
        }
    }

    @Test
    @DisplayName("Получение заказов авторизованного пользователя")
    public void getOrdersForAuthorizedUser() {
        ValidatableResponse response = orderClient.getOrders(accessToken);

        int statusCode = response.extract().statusCode();
        assertThat(statusCode, equalTo(SC_OK));

        boolean isSuccess = response.extract().path("success");
        assertThat(isSuccess, is(true));

        // Проверяем, что список заказов не пустой
        List<Object> orders = response.extract().path("orders");
        assertThat(orders, is(notNullValue()));
    }

    @Test
    @DisplayName("Получение заказов неавторизованного пользователя")
    public void getOrdersForUnauthorizedUser() {
        ValidatableResponse response = orderClient.getOrdersWithoutAuth();

        int statusCode = response.extract().statusCode();
        assertThat(statusCode, equalTo(SC_UNAUTHORIZED));

        boolean isSuccess = response.extract().path("success");
        assertThat(isSuccess, is(false));

        String message = response.extract().path("message");
        assertThat(message, equalTo("You should be authorised"));
    }
}