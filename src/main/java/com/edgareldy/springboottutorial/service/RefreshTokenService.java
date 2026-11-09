package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.entity.token.RefreshToken;
import com.edgareldy.springboottutorial.entity.user.AppUser;

/**
 * Contract for refresh token issuance, validation, and revocation. Called by
 * {@link AuthService} at login (issue), refresh (validate then rotate), and
 * logout (delete).
 * <p>
 * Created edgar.muhamyangabo on 11/9/26
 * Author : edgar.muhamyangabo
 * Date : 11/9/26
 * Project : spring-boot-tutorial
 */
public interface RefreshTokenService {

    RefreshToken generate(AppUser user);

    RefreshToken validate(String rawToken);

    void deleteByUser(AppUser user);
}
