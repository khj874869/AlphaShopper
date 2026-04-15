package com.webjpa.shopping.messaging;

import com.webjpa.shopping.domain.PurchaseOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderNotificationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(OrderNotificationType type, PurchaseOrder order) {
        applicationEventPublisher.publishEvent(OrderNotificationRequested.of(type, order));
    }
}

