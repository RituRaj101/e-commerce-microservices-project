package com.ecommerce.product.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Interview point: by default, Spring's Redis cache manager serializes
 * cached objects using plain JAVA serialization (JDK's ObjectOutputStream)
 * - which is (a) not human-readable if you inspect Redis directly, and
 * (b) requires every cached class to implement Serializable AND stays
 * binary-compatible across restarts, which gets fragile fast.
 *
 * Here we override that to use JSON serialization instead
 * (GenericJackson2JsonRedisSerializer) - readable in Redis CLI/RedisInsight,
 * and matches what the DTOs already look like over HTTP.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // TTL: how long a cached entry lives before Redis auto-expires
                // it, EVEN IF we never explicitly evict it. This is a safety
                // net against permanently stale data if a @CacheEvict is ever
                // missed on some code path.
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues() // don't cache "not found" results
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
