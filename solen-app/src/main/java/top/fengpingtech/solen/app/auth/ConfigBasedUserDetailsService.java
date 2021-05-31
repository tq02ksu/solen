package top.fengpingtech.solen.app.auth;

import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import solen.model.Tenant;
import top.fengpingtech.solen.app.config.AuthProperties;

@Component
public class ConfigBasedUserDetailsService implements UserDetailsService {
    private final AuthProperties tenants;

    public ConfigBasedUserDetailsService(AuthProperties tenants) {
        this.tenants = tenants;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        return tenants.getTenants()
                .stream()
                .filter(t -> t.getAppKey().equals(s))
                .map(t -> {
                    Tenant copy = new Tenant();
                    BeanUtils.copyProperties(t, copy);
                    return copy;
                })
                .map(TenantUserDetails::new)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("user with name " + s + " not found!"));
    }
}
