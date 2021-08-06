package top.fengpingtech.solen.app.auth;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecurityContextFilter implements Filter {
    private static final ThreadLocal<String> PRINCIPAL_HOLDER = new ThreadLocal<>();

    public static void setPrincipal(String principal) {
        PRINCIPAL_HOLDER.set(principal);
    }

    public static String getPrincipal() {
        return PRINCIPAL_HOLDER.get();
    }

    public static void clear() {
        PRINCIPAL_HOLDER.remove();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        try {
            String principal = request.getHeader(SecurityContext.HEADER_PRINCIPAL_NAME);
            if (principal != null) {
                SecurityContext.setPrincipal(principal);
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            SecurityContext.clear();
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
