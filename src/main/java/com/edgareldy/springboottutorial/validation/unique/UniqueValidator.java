package com.edgareldy.springboottutorial.validation.unique;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link ConstraintValidator} implementation for {@link Unique}. Uses
 * {@link EntityManager} and JPQL to query the database generically without
 * depending on a specific repository. Spring's {@code SpringConstraintValidatorFactory}
 * (auto-configured by {@code spring-boot-starter-validation}) manages this
 * class as a Spring bean, so {@code @Autowired} injection of the
 * {@code EntityManager} works here exactly as it does in any service class.
 * {@code required = false} prevents a {@code BeanCreationException} in test
 * slices that have no JPA context (such as {@code @WebMvcTest}); the
 * {@code isValid} method guards against a null {@code EntityManager} and
 * returns {@code true} in that case, letting the database {@code UNIQUE}
 * constraint serve as the final guard.
 * <p>
 * Created edgar.muhamyangabo on 11/8/26
 * Author : edgar.muhamyangabo
 * Date : 11/8/26
 * Project : spring-boot-tutorial
 */
public class UniqueValidator implements ConstraintValidator<Unique, String> {

    @Autowired(required = false)
    private EntityManager entityManager;

    private Class<?> entityClass;
    private String fieldName;
    private boolean caseSensitive;

    @Override
    public void initialize(Unique constraintAnnotation) {
        this.entityClass = constraintAnnotation.entityClass();
        this.fieldName = constraintAnnotation.fieldName();
        this.caseSensitive = constraintAnnotation.caseSensitive();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || entityManager == null) {
            // No JPA context (e.g. @WebMvcTest slice): skip the DB check and
            // let the database UNIQUE constraint be the authoritative guard.
            return true;
        }
        String entityName = entityClass.getSimpleName();
        String jpql = caseSensitive
                ? "SELECT COUNT(e) FROM " + entityName + " e WHERE e." + fieldName + " = :value"
                : "SELECT COUNT(e) FROM " + entityName + " e WHERE LOWER(e." + fieldName + ") = LOWER(:value)";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("value", value)
                .getSingleResult();
        return count == 0;
    }
}
