package com.inditex.core.products.application.usecase;

import com.inditex.core.products.application.port.in.ProductsUseCasePort;
import com.inditex.core.products.application.port.out.ProductsPort;
import com.inditex.core.products.domain.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Resolves the similar products for a given product id.
 * <p>
 * The similar-id lookup is done on the caller's (virtual) thread; the detail
 * of every similar id is then fetched concurrently on its own virtual thread.
 * A similar product whose detail lookup fails or exceeds the configured
 * upstream timeout is silently omitted so one bad dependency never fails
 * the whole request; the remaining products still come back in similarity order.
 */
@Service
class ProductsUseCase implements ProductsUseCasePort {

    private static final Logger log = LoggerFactory.getLogger(ProductsUseCase.class);

    private final ProductsPort productsPort;
    private final ExecutorService productsExecutor;

    ProductsUseCase(ProductsPort productsPort, ExecutorService productsExecutor) {
        this.productsPort = productsPort;
        this.productsExecutor = productsExecutor;
    }

    @Override
    public List<ProductDetail> findProducts(String productId) {
        List<String> productIds = productsPort.getSimilarProductIds(productId);

        List<CompletableFuture<Optional<ProductDetail>>> detailFutures = productIds.stream()
                .map(id -> CompletableFuture.supplyAsync(
                        () -> fetchDetailSafely(id), productsExecutor))
                .toList();

        return detailFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ProductDetail> fetchDetailSafely(String id) {
        try {
            return Optional.of(productsPort.getProductDetail(id));
        } catch (RuntimeException e) {
            log.warn("Omitting product {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }
}
