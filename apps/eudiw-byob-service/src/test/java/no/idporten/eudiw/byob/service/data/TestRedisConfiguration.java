package no.idporten.eudiw.byob.service.data;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
@ActiveProfiles("junit")
public class TestRedisConfiguration {

    private final RedisServer redisServer;

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @Autowired
    public TestRedisConfiguration(RedisProperties redisProperties) throws IOException {
        this.redisServer = new RedisServer(redisProperties.port());
    }

    @PostConstruct
    public void postConstruct() throws IOException {
        System.out.println("postConstruct ports="+ redisServer.ports());
        redisServer.start();
        redisConnectionFactory.getConnection().flushAll();
    }

    @PreDestroy
    public void preDestroy() throws IOException {
        redisServer.stop();
    }
}
