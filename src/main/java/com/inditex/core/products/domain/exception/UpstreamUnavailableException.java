package com.inditex.core.products.domain.exception;

/**
 * The upstream catalog failed, timed out, or returned an unexpected response
 * for a call that is not a plain "not found".
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String operation, String productId, Throwable cause) {
        super("Upstream call '%s' failed for product %s".formatted(operation, productId), cause);
    }
}
