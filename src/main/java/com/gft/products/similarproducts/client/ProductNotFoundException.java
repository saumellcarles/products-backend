package com.gft.products.similarproducts.client;

/**
 * The upstream catalog has no record of the given product id.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
    }

    public ProductNotFoundException(String productId, Throwable cause) {
        super("Product not found: " + productId, cause);
    }
}
