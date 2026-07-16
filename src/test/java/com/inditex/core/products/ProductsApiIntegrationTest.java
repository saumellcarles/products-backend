package com.inditex.core.products;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests mirroring the scenarios described in the assignment
 * (normal, notFound, error, slow, verySlow) against a WireMock stand-in
 * for the existing upstream product catalog.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProductsApiIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void upstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("upstream.base-url", wireMock::baseUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsSimilarProductsInSimilarityOrder() {
        stubSimilarIds("1", "2", "3", "4");
        stubProductDetail("2", "Dress", "19.99", true);
        stubProductDetail("3", "Blazer", "29.99", false);
        stubProductDetail("4", "Boots", "39.99", true);

        ResponseEntity<Map[]> response = restTemplate.getForEntity(similarUrl("1"), Map[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(product -> product.get("id")).containsExactly("2", "3", "4");
    }

    @Test
    void omitsSimilarProductThatDoesNotExist() {
        stubSimilarIds("4", "1", "2", "5");
        stubProductDetail("1", "Shirt", "9.99", true);
        stubProductDetail("2", "Dress", "19.99", true);
        stubProductNotFound("5");

        ResponseEntity<Map[]> response = restTemplate.getForEntity(similarUrl("4"), Map[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(product -> product.get("id")).containsExactly("1", "2");
    }

    @Test
    void omitsSimilarProductWhenUpstreamReturnsServerError() {
        stubSimilarIds("5", "1", "2", "6");
        stubProductDetail("1", "Shirt", "9.99", true);
        stubProductDetail("2", "Dress", "19.99", true);
        wireMock.stubFor(get(urlEqualTo("/product/6")).willReturn(aResponse().withStatus(500)));

        ResponseEntity<Map[]> response = restTemplate.getForEntity(similarUrl("5"), Map[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(product -> product.get("id")).containsExactly("1", "2");
    }

    @Test
    void omitsSimilarProductWhenUpstreamExceedsTheResponseTimeout() {
        stubSimilarIds("3", "1", "100");
        stubProductDetail("1", "Shirt", "9.99", true);
        wireMock.stubFor(get(urlEqualTo("/product/100")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"100\",\"name\":\"Trousers\",\"price\":49.99,\"availability\":false}")
                .withFixedDelay(1500)));

        ResponseEntity<Map[]> response = restTemplate.getForEntity(similarUrl("3"), Map[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(product -> product.get("id")).containsExactly("1");
    }

    @Test
    void returnsNotFoundWhenRootProductHasNoSimilarIds() {
        wireMock.stubFor(get(urlEqualTo("/product/999/similarids")).willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Product not found\"}")));

        ResponseEntity<Map> response = restTemplate.getForEntity(similarUrl("999"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsBadGatewayWhenTheSimilarIdsUpstreamCallFails() {
        wireMock.stubFor(get(urlEqualTo("/product/1/similarids")).willReturn(aResponse().withStatus(500)));

        ResponseEntity<Map> response = restTemplate.getForEntity(similarUrl("1"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void resolvesSimilarProductsConcurrentlyRatherThanSequentially() {
        stubSimilarIds("2", "a", "b", "c");
        for (String id : List.of("a", "b", "c")) {
            wireMock.stubFor(get(urlEqualTo("/product/" + id)).willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withFixedDelay(150)
                    .withBody("{\"id\":\"%s\",\"name\":\"n\",\"price\":1.0,\"availability\":true}".formatted(id))));
        }

        ResponseEntity<Map[]> response = restTemplate.getForEntity(similarUrl("2"), Map[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);

        List<Long> dispatchTimestamps = wireMock.getAllServeEvents().stream()
                .filter(event -> List.of("/product/a", "/product/b", "/product/c").contains(event.getRequest().getUrl()))
                .map(event -> event.getRequest().getLoggedDate().getTime())
                .toList();
        assertThat(dispatchTimestamps).hasSize(3);
        // Asserting on when the 3 upstream calls were dispatched (not on the total
        // response wall-clock time) proves they were fired concurrently regardless of
        // host CPU contention/scheduling overhead, which made a total-duration bound
        // flaky on constrained machines (e.g. a Docker Desktop VM capped at 2 CPUs).
        long dispatchSpreadMs = Collections.max(dispatchTimestamps) - Collections.min(dispatchTimestamps);
        assertThat(dispatchSpreadMs).isLessThan(100);
    }

    private String similarUrl(String productId) {
        return "http://localhost:%d/product/%s/similar".formatted(port, productId);
    }

    private void stubSimilarIds(String productId, String... similarIds) {
        String body = Arrays.stream(similarIds)
                .map(id -> "\"" + id + "\"")
                .reduce((a, b) -> a + "," + b)
                .map(joined -> "[" + joined + "]")
                .orElse("[]");
        wireMock.stubFor(get(urlEqualTo("/product/" + productId + "/similarids")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    private void stubProductDetail(String id, String name, String price, boolean availability) {
        String body = "{\"id\":\"%s\",\"name\":\"%s\",\"price\":%s,\"availability\":%s}"
                .formatted(id, name, price, availability);
        wireMock.stubFor(get(urlEqualTo("/product/" + id)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    private void stubProductNotFound(String id) {
        wireMock.stubFor(get(urlEqualTo("/product/" + id)).willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Product not found\"}")));
    }
}
