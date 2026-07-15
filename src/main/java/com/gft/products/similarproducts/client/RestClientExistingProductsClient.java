package com.gft.products.similarproducts.client;

import com.gft.products.similarproducts.ProductDetail;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
class RestClientExistingProductsClient implements ExistingProductsClient {

    private static final ParameterizedTypeReference<List<String>> SIMILAR_IDS_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient existingProductsRestClient;

    RestClientExistingProductsClient(RestClient existingProductsRestClient) {
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
