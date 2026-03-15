package com.hospitality.order.service;

import com.hospitality.order.dto.CancelOrderRequest;
import com.hospitality.order.dto.CreateOrderRequest;
import com.hospitality.order.dto.ModifyOrderRequest;
import com.hospitality.order.dto.OrderItemRequest;
import com.hospitality.order.dto.OrderResponse;
import com.hospitality.order.exception.OrderNotFoundException;
import com.hospitality.order.exception.OrderStateException;
import com.hospitality.order.model.Order;
import com.hospitality.order.model.OrderItem;
import com.hospitality.order.model.OrderPriority;
import com.hospitality.order.model.OrderStatus;
import com.hospitality.order.model.OrderType;
import com.hospitality.order.model.SagaStatus;
import com.hospitality.order.repository.OrderRepository;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProducerTemplate producerTemplate;

    private OrderServiceImpl orderService;
    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapper();
        orderService = new OrderServiceImpl(orderRepository, orderMapper, producerTemplate);
    }

    @Test
    void shouldCreateOrder() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .travelerId("TRV-001")
                .travelerName("John Doe")
                .propertyId("PROP-001")
                .roomNumber("101")
                .orderType(OrderType.ROOM_SERVICE)
                .items(List.of(
                        OrderItemRequest.builder()
                                .itemId("ITEM-001")
                                .itemName("Club Sandwich")
                                .quantity(2)
                                .unitPrice(15.99)
                                .build()
                ))
                .priority(OrderPriority.NORMAL)
                .build();

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(orderService.createOrder(request))
                .assertNext(response -> {
                    assertThat(response.getOrderId()).isNotNull();
                    assertThat(response.getTravelerId()).isEqualTo("TRV-001");
                    assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(response.getSagaStatus()).isEqualTo(SagaStatus.STARTED);
                    assertThat(response.getTotalAmount()).isEqualTo(31.98);
                })
                .verifyComplete();
    }

    @Test
    void shouldGetOrderById() {
        Order order = buildTestOrder("order-1");
        when(orderRepository.findById("order-1")).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.getOrderById("order-1"))
                .assertNext(response -> {
                    assertThat(response.getOrderId()).isEqualTo("order-1");
                    assertThat(response.getTravelerId()).isEqualTo("TRV-001");
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowNotFoundForMissingOrder() {
        when(orderRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrderById("missing"))
                .expectError(OrderNotFoundException.class)
                .verify();
    }

    @Test
    void shouldModifyOrder() {
        Order order = buildTestOrder("order-2");
        when(orderRepository.findById("order-2")).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        ModifyOrderRequest modifyRequest = ModifyOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .itemId("ITEM-001")
                                .itemName("Club Sandwich")
                                .quantity(3)
                                .unitPrice(15.99)
                                .build()
                ))
                .specialInstructions("Updated instructions")
                .priority(OrderPriority.HIGH)
                .build();

        StepVerifier.create(orderService.modifyOrder("order-2", modifyRequest))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(OrderStatus.MODIFIED);
                    assertThat(response.getTotalAmount()).isEqualTo(47.97);
                    assertThat(response.getSpecialInstructions()).isEqualTo("Updated instructions");
                    assertThat(response.getPriority()).isEqualTo(OrderPriority.HIGH);
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowStateExceptionWhenModifyingCancelledOrder() {
        Order order = buildTestOrder("order-3");
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById("order-3")).thenReturn(Mono.just(order));

        ModifyOrderRequest modifyRequest = ModifyOrderRequest.builder().build();

        StepVerifier.create(orderService.modifyOrder("order-3", modifyRequest))
                .expectError(OrderStateException.class)
                .verify();
    }

    @Test
    void shouldCancelOrder() {
        Order order = buildTestOrder("order-4");
        when(orderRepository.findById("order-4")).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
                .reason("No longer needed")
                .build();

        StepVerifier.create(orderService.cancelOrder("order-4", cancelRequest))
                .assertNext(response -> {
                    assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                    assertThat(response.getCancellationReason()).isEqualTo("No longer needed");
                })
                .verifyComplete();
    }

    @Test
    void shouldGetAllOrders() {
        Order order1 = buildTestOrder("order-a");
        Order order2 = buildTestOrder("order-b");
        when(orderRepository.findAll()).thenReturn(Flux.just(order1, order2));

        StepVerifier.create(orderService.getAllOrders(null, null))
                .expectNextCount(2)
                .verifyComplete();
    }

    private Order buildTestOrder(String orderId) {
        return Order.builder()
                .orderId(orderId)
                .travelerId("TRV-001")
                .travelerName("John Doe")
                .propertyId("PROP-001")
                .roomNumber("101")
                .orderType(OrderType.ROOM_SERVICE)
                .status(OrderStatus.PENDING)
                .sagaStatus(SagaStatus.STARTED)
                .items(List.of(
                        OrderItem.builder()
                                .itemId("ITEM-001")
                                .itemName("Club Sandwich")
                                .quantity(2)
                                .unitPrice(15.99)
                                .subtotal(31.98)
                                .build()
                ))
                .totalAmount(31.98)
                .priority(OrderPriority.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
