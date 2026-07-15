package com.gft.products.similarproducts.client;

import com.gft.products.similarproducts.ProductDetail;

import java.util.List;

/**
 * Port towards the existing (already agreed) product catalog APIs.
 */
public interface ExistingProductsClient {

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
