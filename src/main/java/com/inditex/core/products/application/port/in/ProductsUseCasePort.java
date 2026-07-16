package com.inditex.core.products.application.port.in;

import com.inditex.core.products.domain.exception.ProductNotFoundException;
import com.inditex.core.products.domain.model.ProductDetail;
import com.inditex.core.products.domain.exception.UpstreamUnavailableException;

import java.util.List;

/**
 * Inbound port: what a driving adapter (e.g. the REST controller) can ask the application core to do.
 */
public interface ProductsUseCasePort {

    /**
     * @return the products similar to {@code productId}, ordered by similarity. A similar
     * product whose own detail could not be resolved is omitted rather than failing the request.
     * @throws ProductNotFoundException    if {@code productId} does not exist upstream.
     * @throws UpstreamUnavailableException if the upstream similar-ids lookup fails for a reason other than "not found".
     */
    List<ProductDetail> findProducts(String productId);
}
