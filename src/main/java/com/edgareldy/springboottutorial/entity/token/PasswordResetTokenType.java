package com.edgareldy.springboottutorial.entity.token;

/**
 * Discriminates the two password-reset flows. {@code LINK} is used for the
 * web flow: a UUID token is embedded in a clickable reset link emailed to the
 * user (1-hour TTL). {@code OTP} is used for the mobile flow: a 6-digit
 * numeric code is emailed and must be entered manually in the app before
 * the user can set a new password (5-minute TTL).
 * <p>
 * Created edgar.muhamyangabo on 11/21/26
 * Author : edgar.muhamyangabo
 * Date : 11/21/26
 * Project : spring-boot-tutorial
 */
public enum PasswordResetTokenType {
    LINK,
    OTP
}
