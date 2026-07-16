package com.inditex.core.products.infrastructure.config.concurrency;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
class ProductsExecutorConfig {

    @Bean(destroyMethod = "close")
    ExecutorService productsExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
