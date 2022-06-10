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

//    public Predicate<Connection> filter(Tenant tenant) {
//        return conn -> canVisit(tenant, conn);
//    }

    public boolean canVisit(Tenant tenant, DeviceDomain deviceDomain) {
        return tenant == null || canVisitInternal(tenant, deviceDomain);
    }

    private boolean canVisitInternal(Tenant tenant, DeviceDomain conn) {
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

    public List<String> getPatterns(Tenant tenant, String deviceId) {
        if (tenant == null || tenant.getRoles().contains(ROLE_ADMIN)) {
            return deviceId == null ?
                    Collections.singletonList("**") : Arrays.asList(deviceId.trim().split("[,| ]+"));
        }

        if (deviceId != null && !deviceId.isEmpty()) {
            List<String> patterns = new ArrayList<>();
            for (String id : deviceId.trim().split("[,| ]+")) {
                if (antMatchService.antMatch(tenant.getDevicePatterns(), id)) {
                    patterns.add(id);
                }
            }
            return patterns;
        }

        return tenant.getDevicePatterns();
    }

    public void fillAuthPredicate(Path<String> devicePath, CriteriaBuilder cb, List<Predicate> list) {
        Tenant tenant = getTenant();
        if (tenant != null) {
            javax.persistence.criteria.Predicate[] patternPredicates = tenant.getDevicePatterns().stream()
                    .map(s -> s.replace("**", "%"))
                    .map(s -> cb.like(devicePath, s))
                    .toArray(javax.persistence.criteria.Predicate[]::new);
            list.add(cb.or(patternPredicates));
        }
    }

    public void checkAuth(String deviceId) {

    }
}
