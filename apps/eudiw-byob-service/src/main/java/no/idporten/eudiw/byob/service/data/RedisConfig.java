package no.idporten.eudiw.byob.service.data;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private final Logger log = LoggerFactory.getLogger(RedisConfig.class);
    private final RedisProperties properties;

    public RedisConfig(RedisProperties properties) {
        this.properties = properties;
    }


    @Bean
    LettuceConnectionFactory redisConnectionFactory() {
        if (properties.isSentinal()) {
            return createSentinalConnectionFactory();
        }
        return createStandaloneConnectionFactory();
    }

    private LettuceConnectionFactory createStandaloneConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(properties.host());
        redisStandaloneConfiguration.setPort(properties.port());
        log.info("Single Redis node configured: {}:{}", redisStandaloneConfiguration.getHostName(), redisStandaloneConfiguration.getPort());
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    private LettuceConnectionFactory createSentinalConnectionFactory() {
        RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration();
        sentinelConfiguration.setPassword(properties.password());
        sentinelConfiguration.master(properties.sentinel().master());
        sentinelConfiguration.setSentinelPassword(properties.sentinel().password());
        for (String node : properties.sentinel().nodes()) {
            String[] hostPort = node.split(":");
            sentinelConfiguration.sentinel(hostPort[0], Integer.parseInt(hostPort[1]));
        }
        log.info("Redis Sentinel configured: {}", sentinelConfiguration.getSentinels());
        return new LettuceConnectionFactory(sentinelConfiguration);
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // Use StringRedisSerializer for the keys to keep them human-readable
        template.setKeySerializer(new StringRedisSerializer());

        // Use GenericJackson2JsonRedisSerializer for the values to store objects as JSON strings
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // Also configure hash key/value serializers if needed
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();

        return template;
    }
}