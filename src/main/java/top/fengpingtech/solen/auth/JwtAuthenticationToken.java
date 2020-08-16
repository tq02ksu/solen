package top.fengpingtech.solen.auth;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import top.fengpingtech.solen.model.Tenant;

public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final Tenant tenant;

    public JwtAuthenticationToken(Tenant tenant) {
        super(tenant.getAppKey(), tenant.getPassKey());
        this.tenant = tenant;
    }

    @Override
    public Object getCredentials() {
        return tenant.getPassKey();
    }

    @Override
    public Object getPrincipal() {
        return tenant.getAppKey();
    }
}
