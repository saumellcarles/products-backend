package com.gft.products.similarproducts.adapter.in.web;

import com.gft.products.similarproducts.application.port.in.SimilarProductsUseCase;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
class SimilarProductsController {

    private final SimilarProductsUseCase similarProductsUseCase;

    SimilarProductsController(SimilarProductsUseCase similarProductsUseCase) {
        this.similarProductsUseCase = similarProductsUseCase;
    }

    @GetMapping("/product/{productId}/similar")
    List<ProductDetailResponse> getSimilarProducts(@PathVariable @NotBlank String productId) {
        return similarProductsUseCase.findSimilarProducts(productId).stream()
                .map(ProductDetailResponse::from)
                .toList();
    }
}
