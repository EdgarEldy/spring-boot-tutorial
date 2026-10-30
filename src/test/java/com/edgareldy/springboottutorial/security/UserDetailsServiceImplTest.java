package com.edgareldy.springboottutorial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.entity.Permission;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Unit tests for {@link UserDetailsServiceImpl}, with
 * {@link AppUserRepository} mocked.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void _01_ShouldBuildRoleAndPermissionAuthorities_WhenUserHasRolesWithPermissions() {
        Permission readPerm = Permission.builder().id(1L).resource("product").action("read").build();
        Role adminRole = Role.builder().id(1L).roleName("ADMIN").permissions(Set.of(readPerm)).build();
        AppUser appUser = AppUser.builder()
                .id(1L).username("ada").email("ada@example.com").password("hashed")
                .enabled(true).roles(Set.of(adminRole)).build();
        when(appUserRepository.findByUsername("ada")).thenReturn(Optional.of(appUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("ada");

        assertThat(userDetails.getUsername()).isEqualTo("ada");
        assertThat(userDetails.getPassword()).isEqualTo("hashed");
        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "PERMISSION_PRODUCT:READ");
    }

    @Test
    void _02_ShouldBuildDisabledUserDetails_WhenUserNotEnabled() {
        AppUser appUser = AppUser.builder()
                .id(2L).username("disabled").email("disabled@example.com").password("hashed")
                .enabled(false).roles(Set.of()).build();
        when(appUserRepository.findByUsername("disabled")).thenReturn(Optional.of(appUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void _03_ShouldThrowUsernameNotFound_WhenUserMissing() {
        when(appUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void _04_ShouldBuildLockedUserDetails_WhenUserAccountLocked() {
        AppUser appUser = AppUser.builder()
                .id(3L).username("locked").email("locked@example.com").password("hashed")
                .enabled(true).accountLocked(true).roles(Set.of()).build();
        when(appUserRepository.findByUsername("locked")).thenReturn(Optional.of(appUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("locked");

        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }
}
