package com.bibliotech.bibliotech.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
// order mais alto que o das transações: a invalidação roda DEPOIS do commit.
// Se rodasse antes, uma leitura concorrente poderia repopular o cache com o
// valor antigo.
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig {
}