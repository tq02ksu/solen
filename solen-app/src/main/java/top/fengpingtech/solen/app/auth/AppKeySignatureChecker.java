package top.fengpingtech.solen.app.auth;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class AppKeySignatureChecker {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final MessageSourceAccessor messages = SpringSecurityMessageSource
            .getAccessor();

    void check(AppKeyAuthenticationToken token, TenantUserDetails userDetails) {
        long cost = System.currentTimeMillis() - token.getRequestTime().getTime();
        if (TIMEOUT.minus(Duration.ofMillis(cost)).isNegative()) {
            throw new CredentialsExpiredException(messages.getMessage(
                    "AccountStatusUserDetailsChecker.credentialsExpired",
                    "User credentials have expired"));
        }

        String secretKey = userDetails.getPassword();
        long requestTime = token.getRequestTime().getTime() / 1000;
        String message = String.format("%s%d%s%s", secretKey, requestTime, token.getRequestURI(), secretKey);
        String digest = digest(message);
        if (!digest.equalsIgnoreCase(token.getCredentials().toString())) {
            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"));
        }
    }

    String digest(String message) {
        return DigestUtils.md5DigestAsHex(message.getBytes(StandardCharsets.UTF_8));
    }
}
