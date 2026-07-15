package com.gft.products.similarproducts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
class SimilarProductsExecutorConfig {

    @Bean(destroyMethod = "close")
    ExecutorService similarProductsExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
