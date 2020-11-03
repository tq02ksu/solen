package top.fengpingtech.solen.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AppKeyAuthenticationProvider  implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(AppKeyAuthenticationProvider.class);

    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    private final UserDetailsService userDetailsService;

    private final AppKeySignatureChecker checker;

    public AppKeyAuthenticationProvider(@Qualifier("configBasedUserDetailsService") UserDetailsService userDetailsService, AppKeySignatureChecker checker) {
        this.userDetailsService = userDetailsService;
        this.checker = checker;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String appKey = (authentication.getPrincipal() == null) ? "NONE_PROVIDED"
                : authentication.getName();
        UserDetails user ;
        try {
            user = retrieveUser(appKey, (AppKeyAuthenticationToken) authentication);
        } catch (UsernameNotFoundException notFound) {
            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }

        additionalAuthenticationChecks(user, (AppKeyAuthenticationToken) authentication);

        return createSuccessAuthentication(user, authentication, user);
    }

    private Authentication createSuccessAuthentication(Object principal,
                                                       Authentication authentication, UserDetails user) {
        AppKeyAuthenticationToken token = (AppKeyAuthenticationToken) authentication;
        List<GrantedAuthority> authorities = Optional.ofNullable(user.getAuthorities()).orElseGet(ArrayList::new)
                .stream()
                .map(r -> new SimpleGrantedAuthority(r.getAuthority())).collect(Collectors.toList());
        AppKeyAuthenticationToken result = new AppKeyAuthenticationToken(
             token.getPrincipal().toString(), token.getCredentials().toString(), token.getRequestTime(),
                token.getRequestURI(), authorities);
        result.setDetails(authentication.getDetails());

        return result;
    }

    private UserDetails retrieveUser(String appKey, AppKeyAuthenticationToken authentication) {
        return userDetailsService.loadUserByUsername(appKey);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AppKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private void additionalAuthenticationChecks(UserDetails userDetails,
                                                AppKeyAuthenticationToken authentication)
            throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }

        checker.check(authentication, (TenantUserDetails) userDetails);
    }
}
