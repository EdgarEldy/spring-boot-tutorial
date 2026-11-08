package com.edgareldy.springboottutorial.service;

import lombok.Getter;

/**
 * Enum mapping each transactional email type to its Thymeleaf template name.
 * The {@code name} field matches the HTML file name (without extension) under
 * {@code src/main/resources/templates/}, so Thymeleaf can resolve it directly
 * via {@code SpringTemplateEngine.process(template.getName(), context)}.
 * <p>
 * Created edgar.muhamyangabo on 11/8/26
 * Author : edgar.muhamyangabo
 * Date : 11/8/26
 * Project : spring-boot-tutorial
 */
@Getter
public enum EmailTemplateName {

    ACTIVATE_ACCOUNT("activate_account"),
    RESET_PASSWORD("reset_password");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
