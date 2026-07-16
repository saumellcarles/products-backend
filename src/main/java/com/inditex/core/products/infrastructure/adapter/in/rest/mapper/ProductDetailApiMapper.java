package com.inditex.core.products.infrastructure.adapter.in.rest.mapper;

import com.inditex.core.openapi.model.ProductDetailDTO;
import com.inditex.core.products.domain.model.ProductDetail;
import org.mapstruct.Mapper;

/**
 * Maps the internal domain model onto the model generated from the agreed
 * OpenAPI contract, so the public API never depends on our internal shape.
 */
@Mapper(componentModel = "spring")
public interface ProductDetailApiMapper {

    ProductDetailDTO toApiModel(ProductDetail domain);
}
