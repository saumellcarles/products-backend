package com.gft.products.similarproducts.application.service;

import com.gft.products.similarproducts.application.port.out.ExistingProductsPort;
import com.gft.products.similarproducts.application.port.out.ProductNotFoundException;
import com.gft.products.similarproducts.application.port.out.UpstreamUnavailableException;
import com.gft.products.similarproducts.domain.ProductDetail;
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
class SimilarProductsServiceTest {

    @Mock
    private ExistingProductsPort existingProductsPort;

    private ExecutorService executor;
    private SimilarProductsService service;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        service = new SimilarProductsService(existingProductsPort, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void returnsSimilarProductsInSimilarityOrder() {
        given(existingProductsPort.getSimilarProductIds("1")).willReturn(List.of("2", "3", "4"));
        given(existingProductsPort.getProductDetail("2")).willReturn(productDetail("2"));
        given(existingProductsPort.getProductDetail("3")).willReturn(productDetail("3"));
        given(existingProductsPort.getProductDetail("4")).willReturn(productDetail("4"));

        List<ProductDetail> result = service.findSimilarProducts("1");

        assertThat(result).extracting(ProductDetail::id).containsExactly("2", "3", "4");
    }

    @Test
    void omitsSimilarProductWhenDetailNotFound() {
        given(existingProductsPort.getSimilarProductIds("4")).willReturn(List.of("1", "2", "5"));
        given(existingProductsPort.getProductDetail("1")).willReturn(productDetail("1"));
        given(existingProductsPort.getProductDetail("2")).willReturn(productDetail("2"));
        given(existingProductsPort.getProductDetail("5")).willThrow(new ProductNotFoundException("5"));

        List<ProductDetail> result = service.findSimilarProducts("4");

        assertThat(result).extracting(ProductDetail::id).containsExactly("1", "2");
    }

    @Test
    void omitsSimilarProductWhenUpstreamUnavailable() {
        given(existingProductsPort.getSimilarProductIds("5")).willReturn(List.of("1", "2", "6"));
        given(existingProductsPort.getProductDetail("1")).willReturn(productDetail("1"));
        given(existingProductsPort.getProductDetail("2")).willReturn(productDetail("2"));
        given(existingProductsPort.getProductDetail("6"))
                .willThrow(new UpstreamUnavailableException("detail", "6", new RuntimeException("500")));

        List<ProductDetail> result = service.findSimilarProducts("5");

        assertThat(result).extracting(ProductDetail::id).containsExactly("1", "2");
    }

    @Test
    void returnsEmptyListAndSkipsDetailLookupWhenNoSimilarIds() {
        given(existingProductsPort.getSimilarProductIds("1")).willReturn(List.of());

        List<ProductDetail> result = service.findSimilarProducts("1");

        assertThat(result).isEmpty();
        verify(existingProductsPort, never()).getProductDetail(anyString());
    }

    @Test
    void propagatesProductNotFoundWhenRootLookupFails() {
        given(existingProductsPort.getSimilarProductIds("999")).willThrow(new ProductNotFoundException("999"));

        assertThatThrownBy(() -> service.findSimilarProducts("999"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void propagatesUpstreamUnavailableWhenRootLookupFails() {
        given(existingProductsPort.getSimilarProductIds("1"))
                .willThrow(new UpstreamUnavailableException("similarids", "1", new RuntimeException("boom")));

        assertThatThrownBy(() -> service.findSimilarProducts("1"))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void resolvesSimilarProductDetailsConcurrentlyRatherThanSequentially() {
        given(existingProductsPort.getSimilarProductIds("2")).willReturn(List.of("a", "b", "c"));
        given(existingProductsPort.getProductDetail(anyString())).willAnswer(invocation -> {
            Thread.sleep(300);
            return productDetail(invocation.getArgument(0));
        });

        long start = System.nanoTime();
        List<ProductDetail> result = service.findSimilarProducts("2");
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
