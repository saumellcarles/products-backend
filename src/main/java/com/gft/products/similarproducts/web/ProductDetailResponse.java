package com.gft.products.similarproducts.web;

import com.gft.products.similarproducts.ProductDetail;

import java.math.BigDecimal;

/**
 * Public API representation of a product, decoupled from the upstream client model.
 */
public record ProductDetailResponse(String id, String name, BigDecimal price, boolean availability) {

    public static ProductDetailResponse from(ProductDetail product) {
        return new ProductDetailResponse(product.id(), product.name(), product.price(), product.availability());
    }
}
