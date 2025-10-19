package inventory.example.inventory_id.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Value("${spring.cache.redis.time-to-live:5m}")
  private Duration cacheTimeToLive;

  @Bean
  public RedisTemplate<String, Object> redisTemplate(
    RedisConnectionFactory factory
  ) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();

    Jackson2JsonRedisSerializer<Object> jsonSerializer =
      new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(jsonSerializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(jsonSerializer);
    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();

    Jackson2JsonRedisSerializer<Object> jsonSerializer =
      new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

    RedisCacheConfiguration config =
      RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
          RedisSerializationContext.SerializationPair.fromSerializer(
            jsonSerializer
          )
        )
        .entryTtl(cacheTimeToLive)
        .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(config)
      .build();
  }
}
