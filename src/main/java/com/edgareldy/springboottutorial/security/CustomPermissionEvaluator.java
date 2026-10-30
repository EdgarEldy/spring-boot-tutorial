package com.edgareldy.springboottutorial.security;

import java.io.Serializable;
import java.util.Locale;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Evaluates {@code @PreAuthorize("hasPermission(resource, action)")}
 * expressions against the {@code PERMISSION_<RESOURCE>:<ACTION>} authorities
 * derived from the caller's roles, rather than against a target domain object
 * instance. This lets permission checks stay resource/action based
 * (e.g. {@code hasPermission('PRODUCT', 'WRITE')}) without needing to load
 * an actual entity to check against.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || targetDomainObject == null || permission == null) {
            return false;
        }
        String expectedAuthority = expectedAuthority(targetDomainObject.toString(), permission.toString());
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(expectedAuthority));
    }

    @Override
    public boolean hasPermission(
            Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, targetType, permission);
    }

    private String expectedAuthority(String resource, String action) {
        return "PERMISSION_" + resource.toUpperCase(Locale.ROOT) + ":" + action.toUpperCase(Locale.ROOT);
    }
}
