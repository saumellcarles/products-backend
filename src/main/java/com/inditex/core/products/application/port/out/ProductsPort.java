package com.inditex.core.products.application.port.out;

import com.inditex.core.products.domain.exception.ProductNotFoundException;
import com.inditex.core.products.domain.exception.UpstreamUnavailableException;
import com.inditex.core.products.domain.model.ProductDetail;

import java.util.List;

/**
 * Outbound port towards the existing (already agreed) product catalog APIs.
 * The application core depends only on this abstraction, never on the
 * concrete HTTP integration.
 */
public interface ProductsPort {

    /**
     * @return ids of the products similar to {@code productId}, ordered by similarity.
     * @throws ProductNotFoundException    if {@code productId} does not exist upstream.
     * @throws UpstreamUnavailableException on any other upstream failure (timeout, 5xx, ...).
     */
    List<String> getSimilarProductIds(String productId);

    /**
     * @return the detail of {@code productId}.
     * @throws ProductNotFoundException    if {@code productId} does not exist upstream.
     * @throws UpstreamUnavailableException on any other upstream failure (timeout, 5xx, ...).
     */
    ProductDetail getProductDetail(String productId);
}
