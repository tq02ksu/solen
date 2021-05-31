package top.fengpingtech.solen.app.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import solen.model.Tenant;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtService {
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    private final AuthProperties authProperties;

    private final String jwtSecret;

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        jwtSecret = authProperties.getJwt().getSecret() + UUID.randomUUID().toString();
    }

    /**
     * 生成token
     * @param tenant auth principal
     * @return token string
     */
    public String createToken(Tenant tenant) {

        Map<String, Object> map = new HashMap<>();
        map.put("desc", tenant.getDesc());
        map.put("devicePatterns", tenant.getDevicePatterns());
        map.put("roles", tenant.getRoles());

        return Jwts
                .builder()
                .setId(jwtId())
                .setClaims(map)
                .setIssuedAt(new Date())
                .setIssuer(authProperties.getJwt().getIssuer())
                .setExpiration(new Date(System.currentTimeMillis() + authProperties.getJwt().getTtl()))
                .setSubject(tenant.getAppKey())
                .signWith(SignatureAlgorithm.HS256, jwtSecret).compact();
    }

    private String jwtId() {
        String id = MDC.get("X-Trace-Id");
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    public Tenant parseJwt(String token) {
        Claims c = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token).getBody();

        Tenant t = new Tenant();
        t.setAppKey(c.getSubject());
        t.setDesc(c.get("desc", String.class));
        t.setDevicePatterns(c.get("devicePatterns", List.class));
        t.setRoles(c.get("roles", List.class));
        return t;
    }

    /**
     * 是否过期
     *
     * @param token
     * @return
     */
    public boolean isExpiration(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return claims.getExpiration().before(new Date());
    }
}
