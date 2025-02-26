package com.example.saga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
public class SagaPatternApplication {
    public static void main(String[] args) {
        SpringApplication.run(SagaPatternApplication.class, args);
    }
}

// Order Status Enum
enum OrderStatus { CREATED, PAYMENT_FAILED, INVENTORY_FAILED, COMPLETED, CANCELLED }

// Order Entity
class Order {
    String id;
    OrderStatus status;

    public Order(String id, OrderStatus status) {
        this.id = id;
        this.status = status;
    }
}

// Event Classes
class OrderEvent {
    String orderId;
    OrderStatus status;

    public OrderEvent(String orderId, OrderStatus status) {
        this.orderId = orderId;
        this.status = status;
    }
}

class PaymentEvent {
    String orderId;
    boolean success;

    public PaymentEvent(String orderId, boolean success) {
        this.orderId = orderId;
        this.success = success;
    }
}

class InventoryEvent {
    String orderId;
    boolean success;

    public InventoryEvent(String orderId, boolean success) {
        this.orderId = orderId;
        this.success = success;
    }
}

// Order Controller (Saga Orchestrator)
@RestController
@RequestMapping("/orders")
class OrderController {
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, Order> orders = new HashMap<>();

    public OrderController(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/create")
    public String createOrder() {
        String orderId = UUID.randomUUID().toString();
        orders.put(orderId, new Order(orderId, OrderStatus.CREATED));
        eventPublisher.publishEvent(new OrderEvent(orderId, OrderStatus.CREATED));
        return "Order Created with ID: " + orderId;
    }

    @EventListener
    public void handlePaymentEvent(PaymentEvent event) {
        if (event.success) {
            eventPublisher.publishEvent(new InventoryEvent(event.orderId, true));
        } else {
            orders.get(event.orderId).status = OrderStatus.PAYMENT_FAILED;
            eventPublisher.publishEvent(new OrderEvent(event.orderId, OrderStatus.CANCELLED));
        }
    }

    @EventListener
    public void handleInventoryEvent(InventoryEvent event) {
        if (event.success) {
            orders.get(event.orderId).status = OrderStatus.COMPLETED;
        } else {
            orders.get(event.orderId).status = OrderStatus.INVENTORY_FAILED;
            eventPublisher.publishEvent(new OrderEvent(event.orderId, OrderStatus.CANCELLED));
        }
    }
}

// Payment Service
@Component
class PaymentService {
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void processOrder(OrderEvent event) {
        if (event.status == OrderStatus.CREATED) {
            boolean success = Math.random() > 0.2; // 80% success rate
            eventPublisher.publishEvent(new PaymentEvent(event.orderId, success));
        }
    }
}

// Inventory Service
@Component
class InventoryService {
    private final ApplicationEventPublisher eventPublisher;

    public InventoryService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void reserveInventory(PaymentEvent event) {
        if (event.success) {
            boolean success = Math.random() > 0.3; // 70% success rate
            eventPublisher.publishEvent(new InventoryEvent(event.orderId, success));
        }
    }
}
