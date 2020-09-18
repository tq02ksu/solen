package top.fengpingtech.solen.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.util.List;

@Service
public class AntMatchService {
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final LoadingCache<CacheKey, Boolean> CACHE =
            Caffeine.newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(Duration.ofSeconds(300))
                    .build(key -> antPathMatcher.match(key.pattern, key.deviceId));

    public boolean antMatch(List<String> patterns, String deviceId) {
        return patterns.stream().anyMatch(p -> CACHE.get(CacheKey.builder().pattern(p).deviceId(deviceId).build()));
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class CacheKey {
        private String pattern;
        private String deviceId;
    }
}
