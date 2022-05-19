package top.fengpingtech.solen.app.auth;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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
