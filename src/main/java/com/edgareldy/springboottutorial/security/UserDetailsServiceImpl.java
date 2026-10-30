package com.edgareldy.springboottutorial.security;

import com.edgareldy.springboottutorial.entity.Permission;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads an {@link AppUser} by username and adapts it to Spring Security's
 * {@link UserDetails}, used by the {@code AuthenticationManager} during
 * login and by {@link JwtAuthFilter} to rebuild the authenticated
 * principal on every subsequent request. Builds both {@code ROLE_*} and
 * {@code PERMISSION_RESOURCE:ACTION} authorities so that
 * {@code @PreAuthorize("hasPermission(...)")} expressions evaluated by
 * {@code CustomPermissionEvaluator} work correctly.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username " + username));
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : appUser.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(
                    "ROLE_" + role.getRoleName().toUpperCase(Locale.ROOT)));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(
                        "PERMISSION_" + permission.getResource().toUpperCase(Locale.ROOT)
                                + ":" + permission.getAction().toUpperCase(Locale.ROOT)));
            }
        }
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities(authorities)
                .disabled(!appUser.isEnabled())
                .accountLocked(appUser.isAccountLocked())
                .build();
    }
}
