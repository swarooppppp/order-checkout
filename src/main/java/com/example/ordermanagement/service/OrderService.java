package com.example.ordermanagement.service;

import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.entity.OrderStatus;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    public List<Order> getAllOrders() {
        log.info("Fetching all orders");
        List<Order> orders = orderRepository.findAll();
        log.debug("Found {} orders", orders.size());
        return orders;
    }

    public Order getOrderById(Long id) {
        log.info("Fetching order with id: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found with id: {}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });
    }

    public Order createOrder(Order order) {
        log.info("Creating new order for customer: {}", order.getCustomerId());
        order.setStatus(OrderStatus.CREATED);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with id: {}", savedOrder.getId());
        return savedOrder;
    }

    public Order updateOrder(Long id, Order orderDetails) {
        log.info("Updating order with id: {}", id);
        Order order = getOrderById(id);
        order.setName(orderDetails.getName());
        order.setOriginalAmount(orderDetails.getOriginalAmount());
        order.setFinalAmount(orderDetails.getFinalAmount());
        order.setCustomerId(orderDetails.getCustomerId());
        Order updatedOrder = orderRepository.save(order);
        log.info("Order updated successfully with id: {}", updatedOrder.getId());
        return updatedOrder;
    }

    public Order updateOrderStatus(Long id, OrderStatus status) {
        log.info("Updating order status for id: {} to {}", id, status);
        Order order = getOrderById(id);
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status changed from {} to {} for order id: {}", previousStatus, status, id);
        return updatedOrder;
    }

    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);
        Order order = getOrderById(id);
        orderRepository.delete(order);
        log.info("Order deleted successfully with id: {}", id);
    }

    public List<Order> getOrdersByCustomerId(Long customerId) {
        log.info("Fetching orders for customer id: {}", customerId);
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        log.debug("Found {} orders for customer id: {}", orders.size(), customerId);
        return orders;
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        log.info("Fetching orders with status: {}", status);
        List<Order> orders = orderRepository.findByStatus(status);
        log.debug("Found {} orders with status: {}", orders.size(), status);
        return orders;
    }
}
