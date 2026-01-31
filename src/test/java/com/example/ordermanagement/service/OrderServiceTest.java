package com.example.ordermanagement.service;

import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.entity.OrderStatus;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setName("Test Order");
        testOrder.setOriginalAmount(new BigDecimal("100.00"));
        testOrder.setFinalAmount(new BigDecimal("90.00"));
        testOrder.setStatus(OrderStatus.CREATED);
        testOrder.setCustomerId(1001L);
    }

    @Nested
    @DisplayName("Get Order By ID Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order when ID exists")
        void shouldReturnOrderWhenIdExists() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            Order result = orderService.getOrderById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Test Order", result.getName());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ID does not exist")
        void shouldThrowExceptionWhenIdNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> orderService.getOrderById(999L)
            );

            assertTrue(exception.getMessage().contains("999"));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for null ID")
        void shouldThrowExceptionForNullId() {
            when(orderRepository.findById(null)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(null));
        }
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should set status to CREATED when creating order")
        void shouldSetStatusToCreatedWhenCreatingOrder() {
            testOrder.setStatus(null);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            Order result = orderService.createOrder(testOrder);

            assertEquals(OrderStatus.CREATED, testOrder.getStatus());
            verify(orderRepository, times(1)).save(testOrder);
        }

        @Test
        @DisplayName("Should override any existing status to CREATED")
        void shouldOverrideExistingStatusToCreated() {
            testOrder.setStatus(OrderStatus.PAID);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            orderService.createOrder(testOrder);

            assertEquals(OrderStatus.CREATED, testOrder.getStatus());
        }
    }

    @Nested
    @DisplayName("Update Order Tests")
    class UpdateOrderTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent order")
        void shouldThrowExceptionWhenUpdatingNonExistentOrder() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            Order updateDetails = new Order();
            updateDetails.setName("Updated Name");

            assertThrows(ResourceNotFoundException.class,
                    () -> orderService.updateOrder(999L, updateDetails));
        }

        @Test
        @DisplayName("Should update all fields correctly")
        void shouldUpdateAllFieldsCorrectly() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            Order updateDetails = new Order();
            updateDetails.setName("Updated Order");
            updateDetails.setOriginalAmount(new BigDecimal("200.00"));
            updateDetails.setFinalAmount(new BigDecimal("180.00"));
            updateDetails.setCustomerId(2002L);

            Order result = orderService.updateOrder(1L, updateDetails);

            assertEquals("Updated Order", testOrder.getName());
            assertEquals(new BigDecimal("200.00"), testOrder.getOriginalAmount());
            assertEquals(new BigDecimal("180.00"), testOrder.getFinalAmount());
            assertEquals(2002L, testOrder.getCustomerId());
        }
    }

    @Nested
    @DisplayName("Update Order Status Tests")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should update status from CREATED to PAID")
        void shouldUpdateStatusFromCreatedToPaid() {
            testOrder.setStatus(OrderStatus.CREATED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            Order result = orderService.updateOrderStatus(1L, OrderStatus.PAID);

            assertEquals(OrderStatus.PAID, testOrder.getStatus());
        }

        @Test
        @DisplayName("Should update status from CREATED to CANCELLED")
        void shouldUpdateStatusFromCreatedToCancelled() {
            testOrder.setStatus(OrderStatus.CREATED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            Order result = orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

            assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when updating status of non-existent order")
        void shouldThrowExceptionWhenUpdatingStatusOfNonExistentOrder() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> orderService.updateOrderStatus(999L, OrderStatus.PAID));
        }
    }

    @Nested
    @DisplayName("Delete Order Tests")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should delete order when ID exists")
        void shouldDeleteOrderWhenIdExists() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            doNothing().when(orderRepository).delete(testOrder);

            assertDoesNotThrow(() -> orderService.deleteOrder(1L));
            verify(orderRepository, times(1)).delete(testOrder);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent order")
        void shouldThrowExceptionWhenDeletingNonExistentOrder() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrder(999L));
        }
    }

    @Nested
    @DisplayName("Get Orders By Customer ID Tests")
    class GetOrdersByCustomerIdTests {

        @Test
        @DisplayName("Should return empty list when customer has no orders")
        void shouldReturnEmptyListWhenCustomerHasNoOrders() {
            when(orderRepository.findByCustomerId(9999L)).thenReturn(Collections.emptyList());

            List<Order> result = orderService.getOrdersByCustomerId(9999L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return orders when customer has orders")
        void shouldReturnOrdersWhenCustomerHasOrders() {
            when(orderRepository.findByCustomerId(1001L)).thenReturn(List.of(testOrder));

            List<Order> result = orderService.getOrdersByCustomerId(1001L);

            assertEquals(1, result.size());
            assertEquals(1001L, result.get(0).getCustomerId());
        }
    }

    @Nested
    @DisplayName("Get Orders By Status Tests")
    class GetOrdersByStatusTests {

        @Test
        @DisplayName("Should return empty list when no orders with given status")
        void shouldReturnEmptyListWhenNoOrdersWithStatus() {
            when(orderRepository.findByStatus(OrderStatus.CANCELLED)).thenReturn(Collections.emptyList());

            List<Order> result = orderService.getOrdersByStatus(OrderStatus.CANCELLED);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return all orders with given status")
        void shouldReturnAllOrdersWithStatus() {
            Order order2 = new Order();
            order2.setId(2L);
            order2.setStatus(OrderStatus.CREATED);

            when(orderRepository.findByStatus(OrderStatus.CREATED))
                    .thenReturn(List.of(testOrder, order2));

            List<Order> result = orderService.getOrdersByStatus(OrderStatus.CREATED);

            assertEquals(2, result.size());
        }
    }
}
