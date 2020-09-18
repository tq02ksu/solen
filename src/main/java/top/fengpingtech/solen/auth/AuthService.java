package top.fengpingtech.solen.auth;

import org.springframework.stereotype.Component;
import top.fengpingtech.solen.model.Connection;
import top.fengpingtech.solen.model.Tenant;
import top.fengpingtech.solen.service.AntMatchService;

import java.util.function.Predicate;

@Component
public class AuthService {
    public static final String ROLE_ADMIN = "ADMIN";

    private final AntMatchService antMatchService;

    private final AuthProperties authProperties;

    public AuthService(AntMatchService antMatchService, AuthProperties authProperties) {
        this.antMatchService = antMatchService;
        this.authProperties = authProperties;
    }

    public Predicate<Connection> filter(Tenant tenant) {
        return conn -> canVisit(tenant, conn);
    }

    public boolean canVisit(Tenant tenant, Connection conn) {
        return tenant == null || canVisitInternal(tenant, conn);
    }

    private boolean canVisitInternal(Tenant tenant, Connection conn) {
        if (tenant.getRoles().contains(ROLE_ADMIN)) {
            return true;
        }

        return antMatchService.antMatch(tenant.getDevicePatterns(), conn.getDeviceId());
    }

    public Tenant getTenant(String appKey) {
        if (appKey == null) {
            return null;
        }
        return authProperties.getTenants()
                .stream()
                .filter(t -> t.getAppKey().equalsIgnoreCase(appKey))
                .findFirst().orElseThrow(IllegalStateException::new);
    }
}
