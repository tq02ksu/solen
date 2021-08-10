package top.fengpingtech.solen.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.fengpingtech.solen.app.auth.Tenant;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = AuthProperties.PREFIX)
public class AuthProperties {
    static final String PREFIX = "solen.auth";

    private List<Tenant> tenants = new ArrayList<>();

    private JwtProperties jwt;

    public List<Tenant> getTenants() {
        return tenants;
    }

    public void setTenants(List<Tenant> tenants) {
        this.tenants = tenants;
    }

    public JwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

    public static class JwtProperties {
        private Long ttl;
        private String secret;

        private String issuer;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Long getTtl() {
            return ttl;
        }

        public void setTtl(Long ttl) {
            this.ttl = ttl;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
