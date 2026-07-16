package com.inditex.core.products.infrastructure.adapter.in.rest;

import com.inditex.core.openapi.api.ProductsApi;
import com.inditex.core.openapi.model.ProductDetailDTO;
import com.inditex.core.products.application.port.in.ProductsUseCase;
import com.inditex.core.products.infrastructure.adapter.in.rest.mapper.ProductDetailApiMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Driving adapter implementing the interface generated from the agreed
 * {@code products.yaml} contract (API-first).
 */
@RestController
class ProductsController implements ProductsApi {

    private final ProductsUseCase productsUseCase;
    private final ProductDetailApiMapper productDetailApiMapper;

    ProductsController(ProductsUseCase productsUseCase, ProductDetailApiMapper productDetailApiMapper) {
        this.productsUseCase = productsUseCase;
        this.productDetailApiMapper = productDetailApiMapper;
    }

    @Override
    public ResponseEntity<Set<ProductDetailDTO>> getProductSimilar(String productId) {
        // A LinkedHashSet is required (not a plain HashSet) to preserve the
        // similarity order the contract mandates while still satisfying the
        // generated Set<ProductDetailDTO> return type (uniqueItems: true).
        Set<ProductDetailDTO> products = productsUseCase.findProducts(productId).stream()
                .map(productDetailApiMapper::toApiModel)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return ResponseEntity.ok(products);
    }
}
