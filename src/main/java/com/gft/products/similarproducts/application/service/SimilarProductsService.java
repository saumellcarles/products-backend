package com.gft.products.similarproducts.application.service;

import com.gft.products.similarproducts.application.port.in.SimilarProductsUseCase;
import com.gft.products.similarproducts.application.port.out.ExistingProductsPort;
import com.gft.products.similarproducts.domain.ProductDetail;
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
class SimilarProductsService implements SimilarProductsUseCase {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private final ExistingProductsPort existingProductsPort;
    private final ExecutorService similarProductsExecutor;

    SimilarProductsService(ExistingProductsPort existingProductsPort, ExecutorService similarProductsExecutor) {
        this.existingProductsPort = existingProductsPort;
        this.similarProductsExecutor = similarProductsExecutor;
    }

    @Override
    public List<ProductDetail> findSimilarProducts(String productId) {
        List<String> similarProductIds = existingProductsPort.getSimilarProductIds(productId);

        List<CompletableFuture<Optional<ProductDetail>>> detailFutures = similarProductIds.stream()
                .map(similarProductId -> CompletableFuture.supplyAsync(
                        () -> fetchDetailSafely(similarProductId), similarProductsExecutor))
                .toList();

        return detailFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ProductDetail> fetchDetailSafely(String similarProductId) {
        try {
            return Optional.of(existingProductsPort.getProductDetail(similarProductId));
        } catch (RuntimeException e) {
            log.warn("Omitting similar product {}: {}", similarProductId, e.getMessage());
            return Optional.empty();
        }
    }
}
