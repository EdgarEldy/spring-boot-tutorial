package com.edgareldy.springboottutorial.entity;

/**
 * Lifecycle statuses for an e-commerce order.
 * PENDING - created by checkout, awaiting admin confirmation.
 * CONFIRMED - confirmed by an administrator.
 * CANCELLED - cancelled by admin or system.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
