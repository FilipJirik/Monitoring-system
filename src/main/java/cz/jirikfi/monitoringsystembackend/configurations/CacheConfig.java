package cz.jirikfi.monitoringsystembackend.configurations;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DEVICE_THRESHOLDS = "deviceThresholds";
    public static final String SYSTEM_SETTINGS = "systemSettings";
    public static final String DEVICE_API_KEYS = "deviceApiKeys";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                DEVICE_THRESHOLDS,
                SYSTEM_SETTINGS,
                DEVICE_API_KEYS);
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(300)
                .maximumSize(1000)
                .recordStats()
                .expireAfterWrite(Duration.ofMinutes(10));
    }
}
