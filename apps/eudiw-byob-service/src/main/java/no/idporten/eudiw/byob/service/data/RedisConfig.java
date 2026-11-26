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
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

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

        // Values are stored as JSON with type info (@class), so they deserialize back to the original class
        GenericJacksonJsonRedisSerializer valueSerializer = jsonSerializerWithTypeInfo();
        template.setValueSerializer(valueSerializer);

        // Also configure hash key/value serializers if needed
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();

        return template;
    }

    private static GenericJacksonJsonRedisSerializer jsonSerializerWithTypeInfo() {
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("no.idporten.eudiw.byob.service.model.data.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .build();
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
    }
}
