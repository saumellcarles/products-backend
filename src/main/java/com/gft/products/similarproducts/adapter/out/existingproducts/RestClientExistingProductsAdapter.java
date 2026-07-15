package com.gft.products.similarproducts.adapter.out.existingproducts;

import com.gft.products.similarproducts.application.port.out.ExistingProductsPort;
import com.gft.products.similarproducts.application.port.out.ProductNotFoundException;
import com.gft.products.similarproducts.application.port.out.UpstreamUnavailableException;
import com.gft.products.similarproducts.domain.ProductDetail;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Driven adapter implementing {@link ExistingProductsPort} over Spring's {@link RestClient}.
 */
@Component
class RestClientExistingProductsAdapter implements ExistingProductsPort {

    private static final ParameterizedTypeReference<List<String>> SIMILAR_IDS_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient existingProductsRestClient;

    RestClientExistingProductsAdapter(RestClient existingProductsRestClient) {
        this.existingProductsRestClient = existingProductsRestClient;
    }

    @Override
    public List<String> getSimilarProductIds(String productId) {
        try {
            List<String> similarIds = existingProductsRestClient.get()
                    .uri("/product/{productId}/similarids", productId)
                    .retrieve()
                    .body(SIMILAR_IDS_TYPE);
            return similarIds != null ? similarIds : List.of();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId, e);
        } catch (RestClientException e) {
            throw new UpstreamUnavailableException("similarids", productId, e);
        }
    }

    @Override
    public ProductDetail getProductDetail(String productId) {
        try {
            ProductDetail detail = existingProductsRestClient.get()
                    .uri("/product/{productId}", productId)
                    .retrieve()
                    .body(ProductDetail.class);
            if (detail == null) {
                throw new ProductNotFoundException(productId);
            }
            return detail;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId, e);
        } catch (RestClientException e) {
            throw new UpstreamUnavailableException("detail", productId, e);
        }
    }
}
