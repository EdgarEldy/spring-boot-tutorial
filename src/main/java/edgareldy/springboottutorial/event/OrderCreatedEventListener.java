package edgareldy.springboottutorial.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumes {@link OrderCreatedEvent} for business logging, and stands as
 * the extension point for a future notification (e.g. order confirmation
 * email). Uses {@code @TransactionalEventListener} rather than plain
 * {@code @EventListener} so it only runs after the transaction that
 * created the order has actually committed: a synchronous
 * {@code @EventListener} would fire mid-transaction, before the order row
 * is durable, which would log a creation that a later rollback could still
 * undo.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Component
public class OrderCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Order {} created for customer {} on product {}: quantity={}, total={}",
                event.orderId(), event.customerId(), event.productId(), event.quantity(), event.total());
    }
}
