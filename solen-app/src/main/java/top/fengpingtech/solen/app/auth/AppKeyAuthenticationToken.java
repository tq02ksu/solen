package top.fengpingtech.solen.app.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Date;
import java.util.List;

public class AppKeyAuthenticationToken extends AbstractAuthenticationToken {
    private String appKey;

    private String sign;

    private Date requestTime;

    private String requestURI;

    public AppKeyAuthenticationToken(String appKey, String sign, Date requestTime, String requestURI) {
        super(null);
        this.appKey = appKey;
        this.sign = sign;
        this.requestTime = requestTime;
        this.requestURI = requestURI;
    }

    public AppKeyAuthenticationToken(String appKey, String sign, Date requestTime, String requestURI, List<GrantedAuthority> authorities) {
        super(authorities);
        this.appKey = appKey;
        this.sign = sign;
        this.requestTime = requestTime;
        this.requestURI = requestURI;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return sign;
    }

    @Override
    public Object getPrincipal() {
        return appKey;
    }

    public Date getRequestTime() {
        return requestTime;
    }

    public String getRequestURI() {
        return requestURI;
    }
}
