package com.example.orderservice.service;

import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.OrderEvent;
import com.example.orderservice.event.OrderEventType;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Business logic for orders, including calling USER-SERVICE and computing totalAmount. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    // Used to announce events. OrderEventPublisher sends them to Kafka after the DB commit.
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for userId={}", request.getUserId());
        userClient.validateUserExists(request.getUserId()); // fails fast if the user is missing

        Order order = Order.builder()
                .userId(request.getUserId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .totalAmount(calculateTotal(request))
                .orderStatus(request.getOrderStatus() != null ? request.getOrderStatus() : OrderStatus.PENDING)
                .build();
        Order saved = orderRepository.save(order);
        log.info("Order created id={} total={}", saved.getId(), saved.getTotalAmount());
        eventPublisher.publishEvent(toEvent(saved, OrderEventType.ORDER_CREATED)); // -> Kafka after commit
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order id={}", id);
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("Fetching orders for userId={}", userId);
        return orderRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        log.info("Updating order id={}", id);
        Order order = findOrThrow(id);
        // Only call USER-SERVICE again if the order is being moved to a different user
        if (!order.getUserId().equals(request.getUserId())) {
            userClient.validateUserExists(request.getUserId());
        }
        order.setUserId(request.getUserId());
        order.setProductName(request.getProductName());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setTotalAmount(calculateTotal(request));
        if (request.getOrderStatus() != null) {
            order.setOrderStatus(request.getOrderStatus());
        }
        Order updated = orderRepository.save(order);
        eventPublisher.publishEvent(toEvent(updated, OrderEventType.ORDER_UPDATED)); // -> Kafka after commit
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order id={}", id);
        Order order = findOrThrow(id);
        orderRepository.delete(order);
        eventPublisher.publishEvent(toEvent(order, OrderEventType.ORDER_DELETED)); // -> Kafka after commit
    }

    /** totalAmount = quantity x price, always computed on the server. */
    private BigDecimal calculateTotal(OrderRequest r) {
        return r.getPrice().multiply(BigDecimal.valueOf(r.getQuantity()));
    }

    private Order findOrThrow(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> {
            log.warn("Order not found id={}", id);
            return new OrderNotFoundException("Order not found with id " + id);
        });
    }

    /** Builds the Kafka message from the saved order. */
    private OrderEvent toEvent(Order o, OrderEventType type) {
        return OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(type)
                .orderId(o.getId())
                .userId(o.getUserId())
                .productName(o.getProductName())
                .quantity(o.getQuantity())
                .price(o.getPrice())
                .totalAmount(o.getTotalAmount())
                .orderStatus(o.getOrderStatus())
                .occurredAt(Instant.now())
                .build();
    }

    private OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .productName(o.getProductName())
                .quantity(o.getQuantity())
                .price(o.getPrice())
                .totalAmount(o.getTotalAmount())
                .orderStatus(o.getOrderStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
