package top.fengpingtech.solen.auth;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.Tenant;

import java.time.Duration;
import java.util.function.Predicate;

@Component
public class AuthService {
    private static final String ROLE_ADMIN = "ADMIN";

    private final LoadingCache<CacheKey, Boolean> cache =
            Caffeine.newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(Duration.ofSeconds(300))
                    .build(key -> canVisitInternal(key.tenant, key.conn));

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public Predicate<Connection> filter(Tenant tenant) {
        return conn -> canVisit(tenant, conn);
    }

    public boolean canVisit(Tenant tenant, Connection conn) {
        return tenant == null || cache.get(new CacheKey(tenant, conn));
    }

    private boolean canVisitInternal(Tenant tenant, Connection conn) {
        if (tenant.getRoles().contains(ROLE_ADMIN)) {
            return true;
        }

        return tenant.getDevicePatterns()
                .stream()
                .anyMatch(p -> antMatch(p, conn.getDeviceId()));
    }

    private boolean antMatch(String pattern, String deviceId) {
        return antPathMatcher.match(pattern, deviceId);
    }

    private static class CacheKey {
        private Tenant tenant;
        private Connection conn;
        public CacheKey(Tenant tenant, Connection conn) {
            this.tenant = tenant;
            this.conn = conn;
        }
    }
}
