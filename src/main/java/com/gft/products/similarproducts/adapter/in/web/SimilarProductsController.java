package com.gft.products.similarproducts.adapter.in.web;

import com.gft.products.openapi.api.SimilarProductsApi;
import com.gft.products.openapi.model.ProductDetail;
import com.gft.products.similarproducts.application.port.in.SimilarProductsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Driving adapter implementing the interface generated from the agreed
 * {@code similarProducts.yaml} contract (API-first).
 */
@RestController
class SimilarProductsController implements SimilarProductsApi {

    private final SimilarProductsUseCase similarProductsUseCase;

    SimilarProductsController(SimilarProductsUseCase similarProductsUseCase) {
        this.similarProductsUseCase = similarProductsUseCase;
    }

    @Override
    public ResponseEntity<Set<ProductDetail>> getProductSimilar(String productId) {
        // A LinkedHashSet is required (not a plain HashSet) to preserve the
        // similarity order the contract mandates while still satisfying the
        // generated Set<ProductDetail> return type (uniqueItems: true).
        Set<ProductDetail> similarProducts = similarProductsUseCase.findSimilarProducts(productId).stream()
                .map(ProductDetailApiMapper::toApiModel)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return ResponseEntity.ok(similarProducts);
    }
}
