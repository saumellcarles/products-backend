package com.gft.products.similarproducts.adapter.in.web;

import com.gft.products.similarproducts.domain.ProductDetail;

/**
 * Maps the internal domain model onto the model generated from the agreed
 * OpenAPI contract, so the public API never depends on our internal shape.
 */
final class ProductDetailApiMapper {

    private ProductDetailApiMapper() {
    }

    static com.gft.products.openapi.model.ProductDetail toApiModel(ProductDetail domain) {
        return new com.gft.products.openapi.model.ProductDetail(
                domain.id(), domain.name(), domain.price(), domain.availability());
    }
}
