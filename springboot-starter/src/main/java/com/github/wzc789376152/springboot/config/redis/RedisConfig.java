package com.github.wzc789376152.springboot.config.redis;

import com.github.wzc789376152.springboot.config.redis.service.RedisPlatformService;
import jodd.util.StringUtil;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis配置
 *
 * @author jackw
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "wzc.redis", name = "enable", havingValue = "true")
@ConfigurationProperties(prefix = "wzc.redis")
public class RedisConfig extends CachingConfigurerSupport {
    private Boolean enable = false;
    @Value("${spring.redis.host:}")
    private String host;

    @Value("${spring.redis.port:}")
    private Long port;

    @Value("${spring.redis.password:}")
    private String password;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public Redisson redisson() {
        if (!this.enable) {
            return null;
        }
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        if (StringUtil.isNotEmpty(password)) {
            config.useSingleServer().setPassword(password);
        }
        return (Redisson) Redisson.create(config);
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplate() {
        if (!this.enable) {
            return null;
        }
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(this.redisConnectionFactory);
        FastJson2JsonRedisPlatformSerializer<Object> serializer = new FastJson2JsonRedisPlatformSerializer<>(Object.class);
        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public IRedisService redisService() {
        RedisTemplate<Object, Object> redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return null;
        }
        return new RedisPlatformService(redisTemplate);
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }
}