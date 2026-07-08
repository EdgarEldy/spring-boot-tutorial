package edgareldy.springboottutorial.event;

/**
 * Application event published by {@code OrderServiceImpl} once a new order
 * has been persisted. A plain record, not a subclass of
 * {@code ApplicationEvent}: Spring has supported arbitrary event payload
 * types since 4.2, and a bare {@code record} keeps this event free of any
 * framework coupling beyond the {@code @EventListener} contract on the
 * consuming side.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        Long productId,
        int quantity,
        double total
) {
}
