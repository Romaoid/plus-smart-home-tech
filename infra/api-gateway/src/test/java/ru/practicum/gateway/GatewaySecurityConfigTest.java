package ru.practicum.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(classes = {ApiGateway.class, GatewaySecurityConfigTest.TestBackendConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void catalogGet_isPublic() {
        webTestClient
                .get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void orderCreate_withoutCredentials_isUnauthorized() {
        // TODO: проверьте, что POST /api/orders без учётных данных возвращает 401
        webTestClient
                .post()
                .uri("/api/products")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void orderCreate_withUserCredentials_isOk() {
        // TODO: проверьте, что POST /api/orders без учётных данных возвращает 401
        webTestClient
                .post()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan","ivan"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void productWrite_withUserCredentials_isForbidden() {
        // TODO: проверьте, что USER не может выполнять write-операцию с каталогом
        webTestClient
                .patch()
                .uri("/api/products/1")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan","ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void productWrite_withAdminCredentials_passesSecurity() {
        // TODO: проверьте, что ADMIN проходит security-проверку для write-операции
        webTestClient
                .patch()
                .uri("/api/products/1")
                .header(HttpHeaders.AUTHORIZATION, basic("anna","anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void ordersGet_withUserCredentials_isForbidden() {
        webTestClient
                .get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan","ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void ordersGet_withAdminCredentials_passesSecurity() {
        webTestClient
                .get()
                .uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("anna","anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unknownRoute_withAdminCredentials_isForbidden() {
        // TODO: проверьте, что неизвестный маршрут закрыт даже для ADMIN
        webTestClient
                .post()
                .uri("/root")
                .header(HttpHeaders.AUTHORIZATION, basic("anna","anna"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void corsPreflight_isPublic() {
        // TODO: проверьте, что OPTIONS /api/orders не блокируется security-слоем
        webTestClient
                .options()
                .uri("/api/orders")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk();
    }

    private String basic(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route(path("/api/**"), request -> ServerResponse.ok().build());
        }
    }
}
