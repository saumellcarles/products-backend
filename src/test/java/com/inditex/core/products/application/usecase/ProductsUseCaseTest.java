package com.inditex.core.products.application.usecase;

import com.inditex.core.products.application.port.out.ProductsPort;
import com.inditex.core.products.domain.exception.ProductNotFoundException;
import com.inditex.core.products.domain.exception.UpstreamUnavailableException;
import com.inditex.core.products.domain.model.ProductDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductsUseCaseTest {

    @Mock
    private ProductsPort productsPort;

    private ExecutorService executor;
    private ProductsUseCase service;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        service = new ProductsUseCase(productsPort, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void returnsSimilarProductsInSimilarityOrder() {
        given(productsPort.getSimilarProductIds("1")).willReturn(List.of("2", "3", "4"));
        given(productsPort.getProductDetail("2")).willReturn(productDetail("2"));
        given(productsPort.getProductDetail("3")).willReturn(productDetail("3"));
        given(productsPort.getProductDetail("4")).willReturn(productDetail("4"));

        List<ProductDetail> result = service.findProducts("1");

        assertThat(result).extracting(ProductDetail::id).containsExactly("2", "3", "4");
    }

    @Test
    void omitsSimilarProductWhenDetailNotFound() {
        given(productsPort.getSimilarProductIds("4")).willReturn(List.of("1", "2", "5"));
        given(productsPort.getProductDetail("1")).willReturn(productDetail("1"));
        given(productsPort.getProductDetail("2")).willReturn(productDetail("2"));
        given(productsPort.getProductDetail("5")).willThrow(new ProductNotFoundException("5"));

        List<ProductDetail> result = service.findProducts("4");

        assertThat(result).extracting(ProductDetail::id).containsExactly("1", "2");
    }

    @Test
    void omitsSimilarProductWhenUpstreamUnavailable() {
        given(productsPort.getSimilarProductIds("5")).willReturn(List.of("1", "2", "6"));
        given(productsPort.getProductDetail("1")).willReturn(productDetail("1"));
        given(productsPort.getProductDetail("2")).willReturn(productDetail("2"));
        given(productsPort.getProductDetail("6"))
                .willThrow(new UpstreamUnavailableException("detail", "6", new RuntimeException("500")));

        List<ProductDetail> result = service.findProducts("5");

        assertThat(result).extracting(ProductDetail::id).containsExactly("1", "2");
    }

    @Test
    void returnsEmptyListAndSkipsDetailLookupWhenNoSimilarIds() {
        given(productsPort.getSimilarProductIds("1")).willReturn(List.of());

        List<ProductDetail> result = service.findProducts("1");

        assertThat(result).isEmpty();
        verify(productsPort, never()).getProductDetail(anyString());
    }

    @Test
    void propagatesProductNotFoundWhenRootLookupFails() {
        given(productsPort.getSimilarProductIds("999")).willThrow(new ProductNotFoundException("999"));

        assertThatThrownBy(() -> service.findProducts("999"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void propagatesUpstreamUnavailableWhenRootLookupFails() {
        given(productsPort.getSimilarProductIds("1"))
                .willThrow(new UpstreamUnavailableException("similarids", "1", new RuntimeException("boom")));

        assertThatThrownBy(() -> service.findProducts("1"))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void resolvesSimilarProductDetailsConcurrentlyRatherThanSequentially() {
        given(productsPort.getSimilarProductIds("2")).willReturn(List.of("a", "b", "c"));
        given(productsPort.getProductDetail(anyString())).willAnswer(invocation -> {
            Thread.sleep(300);
            return productDetail(invocation.getArgument(0));
        });

        long start = System.nanoTime();
        List<ProductDetail> result = service.findProducts("2");
        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(result).hasSize(3);
        // Sequential fan-out of 3 x 300ms would take ~900ms+; a generous 700ms bound
        // is enough to prove the calls ran concurrently without being timing-flaky.
        assertThat(elapsedMs).isLessThan(700);
    }

    private static ProductDetail productDetail(String id) {
        return new ProductDetail(id, "name-" + id, BigDecimal.TEN, true);
    }
}
