package top.fengpingtech.solen.app.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class AppKeyAuthorizationFilter extends BasicAuthenticationFilter {

    public AppKeyAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String sign = request.getParameter("sign");
        String requestTime = request.getParameter("requestTime");
        String appKey = request.getParameter("appKey");

        if (sign != null && requestTime != null && appKey != null) {
            String uri = request.getRequestURI();
            SecurityContextHolder.getContext().setAuthentication(getAuthentication(sign, requestTime, appKey, uri));
            String principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
            try {
                super.doFilterInternal(
                        new HeadersOverrideRequest(request, "Authorization-Principal", principal),
                        response, chain);

            } finally {
                SecurityContextHolder.getContext().setAuthentication(null);
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private Authentication getAuthentication(String sign, String requestTime, String appKey, String uri) {
        return new AppKeyAuthenticationToken(appKey, sign, new Date(Long.parseLong(requestTime) * 1000), uri);
    }
}
