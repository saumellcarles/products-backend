package com.gft.products.similarproducts.application.port.in;

import com.gft.products.similarproducts.domain.ProductDetail;

import java.util.List;

/**
 * Inbound port: what a driving adapter (e.g. the REST controller) can ask the application core to do.
 */
public interface SimilarProductsUseCase {

    /**
     * @return the products similar to {@code productId}, ordered by similarity. A similar
     * product whose own detail could not be resolved is omitted rather than failing the request.
     * @throws com.gft.products.similarproducts.application.port.out.ProductNotFoundException    if {@code productId} does not exist upstream.
     * @throws com.gft.products.similarproducts.application.port.out.UpstreamUnavailableException if the upstream similar-ids lookup fails for a reason other than "not found".
     */
    List<ProductDetail> findSimilarProducts(String productId);
}
