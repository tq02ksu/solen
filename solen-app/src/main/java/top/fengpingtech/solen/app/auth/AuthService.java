package top.fengpingtech.solen.app.auth;

import org.springframework.stereotype.Component;
import top.fengpingtech.solen.app.config.AuthProperties;
import top.fengpingtech.solen.app.domain.DeviceDomain;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class AuthService {
    public static final String ROLE_ADMIN = "ADMIN";

    private final AntMatchService antMatchService;

    private final AuthProperties authProperties;

    public AuthService(AntMatchService antMatchService, AuthProperties authProperties) {
        this.antMatchService = antMatchService;
        this.authProperties = authProperties;
    }

    public boolean canVisit(DeviceDomain conn) {
        Tenant tenant = getTenant();

        if (tenant.getRoles().contains(ROLE_ADMIN)) {
            return true;
        }

        return antMatchService.antMatch(tenant.getDevicePatterns(), conn.getDeviceId());
    }

    public Tenant getTenant() {
        String principal = SecurityContext.getPrincipal();
        if (principal == null) {
            return null;
        }
        return authProperties.getTenants()
                .stream()
                .filter(t -> t.getAppKey().equalsIgnoreCase(principal))
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    public void fillAuthPredicate(Path<String> devicePath, CriteriaBuilder cb, List<Predicate> list) {
        Tenant tenant = getTenant();
        if (tenant == null || tenant.getRoles().contains(ROLE_ADMIN)) {
            return;
        }

        Predicate[] patternPredicates = tenant.getDevicePatterns().stream()
                .map(s -> s.replace("**", "%"))
                .map(s -> cb.like(devicePath, s))
                .toArray(javax.persistence.criteria.Predicate[]::new);
        list.add(cb.or(patternPredicates));
    }
}
