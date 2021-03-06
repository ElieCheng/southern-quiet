package me.insidezhou.southernquiet.throttle;

import me.insidezhou.southernquiet.throttle.lua.RedisLuaThrottleManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class RedisThrottleAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RedisLuaThrottleManager redisThrottleManager(StringRedisTemplate stringRedisTemplate) {
        return new RedisLuaThrottleManager(stringRedisTemplate);
    }
}
