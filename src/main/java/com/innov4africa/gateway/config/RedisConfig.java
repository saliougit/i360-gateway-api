package com.innov4africa.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class RedisConfig {
    
    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    public Counter redisDownCounter() {
        return Counter.builder("gateway.redis.down")
                .description("Number of times Redis was down")
                .register(meterRegistry);
    }

    @Bean
    public Counter fallbackJwtValidationCounter() {
        return Counter.builder("gateway.jwt.fallback")
                .description("Number of JWT fallback validations")
                .register(meterRegistry);
    }   
     
    @Bean("tokenRedisTemplate")
    @Primary
    public ReactiveRedisTemplate<String, String> tokenRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        RedisSerializer<String> serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
            .<String, String>newSerializationContext()
            .key(serializer)
            .value(serializer)
            .hashKey(serializer)
            .hashValue(serializer)
            .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
