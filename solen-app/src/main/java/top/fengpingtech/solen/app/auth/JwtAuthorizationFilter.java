package top.fengpingtech.solen.app.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import solen.model.Tenant;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {
    private final JwtService jwtService;
    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtService jwtService) {
        super(authenticationManager);
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String tokenHeader = request.getHeader(JwtService.TOKEN_HEADER);

        if (tokenHeader != null && tokenHeader.startsWith(JwtService.TOKEN_PREFIX)) {
            Authentication authentication = getAuthentication(tokenHeader);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String principal = authentication == null ? null : authentication.getPrincipal().toString();
            try {
                request = principal == null ? request
                        : new HeadersOverrideRequest(request, "Authorization-Principal", principal);
                super.doFilterInternal(request, response, chain);
            } finally {
                SecurityContextHolder.getContext().setAuthentication(null);
            }
            return;
        }

        // 如果请求头中没有Authorization信息则直接放行了
        chain.doFilter(request, response);
    }

    // 这里从token中获取用户信息并新建一个token
    private UsernamePasswordAuthenticationToken getAuthentication(String tokenHeader) {
        String token = tokenHeader.replace(JwtService.TOKEN_PREFIX, "");
        Tenant t = jwtService.parseJwt(token);
        if (t.getAppKey() != null){
            return new UsernamePasswordAuthenticationToken(t.getAppKey(), null,
                    Optional.ofNullable(t.getRoles()).orElseGet(ArrayList::new)
                            .stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
        }
        return null;
    }
}
