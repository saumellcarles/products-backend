package com.inditex.core.products.domain.model;

import java.math.BigDecimal;

/**
 * Product detail as resolved from the upstream product catalog.
 */
public record ProductDetail(String id, String name, BigDecimal price, boolean availability) {
}