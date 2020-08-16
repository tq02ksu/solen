package top.fengpingtech.solen.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.fengpingtech.solen.model.Tenant;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class JwtAuthenticationTokenFilter  extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public JwtAuthenticationTokenFilter(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        setFilterProcessesUrl("/authenticate");
    }
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {

        // 从输入流中获取到登录的信息
        try (InputStream in = request.getInputStream()) {
            Tenant tenant = new ObjectMapper().readValue(in, Tenant.class);
            return authenticationManager.authenticate(new JwtAuthenticationToken(tenant));
        } catch (IOException e) {
            throw new AuthenticationServiceException("error while authentication", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        TenantUserDetails userDetails = (TenantUserDetails) authResult.getPrincipal();

        String token = jwtService.createToken(userDetails.getTenant());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        String tokenStr = JwtService.TOKEN_PREFIX + token;
        response.setHeader(JwtService.TOKEN_HEADER, tokenStr);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(String.format("{\"status\": 200,\"token\": \"%s\"}", token));
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.setStatus(401);
        try (PrintWriter writer = response.getWriter()) {
            String body = String.format("{\"status\": 401,\"token\": \"%s\"}", failed.getMessage());
            writer.write(body);
        }
    }
}
