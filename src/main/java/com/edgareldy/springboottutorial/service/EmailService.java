package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.entity.user.AppUser;

/**
 * Contract for sending account-related transactional emails. Implementations
 * run asynchronously (see {@code AsyncConfig}) so a slow SMTP call never
 * blocks the HTTP request thread that triggered it.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface EmailService {

    void sendActivationEmail(AppUser user, String token);

    void sendPasswordResetEmail(AppUser user, String token);
}
