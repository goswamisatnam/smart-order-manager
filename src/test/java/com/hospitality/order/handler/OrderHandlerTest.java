package com.hospitality.order.handler;

import com.hospitality.order.dto.CancelOrderRequest;
import com.hospitality.order.dto.CreateOrderRequest;
import com.hospitality.order.dto.ModifyOrderRequest;
import com.hospitality.order.dto.OrderItemRequest;
import com.hospitality.order.dto.OrderItemResponse;
import com.hospitality.order.dto.OrderResponse;
import com.hospitality.order.model.DietaryPreference;
import com.hospitality.order.model.OrderPriority;
import com.hospitality.order.model.OrderStatus;
import com.hospitality.order.model.OrderType;
import com.hospitality.order.model.SagaStatus;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    private static String createdOrderId;

    @Test
    @Order(1)
    void shouldCreateOrder() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .travelerId("TRV-001")
                .travelerName("John Doe")
                .propertyId("PROP-HILTON-NYC-001")
                .roomNumber("1205")
                .orderType(OrderType.ROOM_SERVICE)
                .items(List.of(
                        OrderItemRequest.builder()
                                .itemId("ITEM-001")
                                .itemName("Club Sandwich")
                                .quantity(2)
                                .unitPrice(15.99)
                                .customizations("No onions")
                                .build(),
                        OrderItemRequest.builder()
                                .itemId("ITEM-002")
                                .itemName("Caesar Salad")
                                .quantity(1)
                                .unitPrice(12.50)
                                .build()
                ))
                .specialInstructions("Please deliver after 7 PM")
                .dietaryPreferences(List.of(DietaryPreference.GLUTEN_FREE))
                .priority(OrderPriority.NORMAL)
                .build();

        webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.getOrderId()).isNotNull();
                    assertThat(response.getTravelerId()).isEqualTo("TRV-001");
                    assertThat(response.getTravelerName()).isEqualTo("John Doe");
                    assertThat(response.getPropertyId()).isEqualTo("PROP-HILTON-NYC-001");
                    assertThat(response.getRoomNumber()).isEqualTo("1205");
                    assertThat(response.getOrderType()).isEqualTo(OrderType.ROOM_SERVICE);
                    assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(response.getItems()).hasSize(2);
                    assertThat(response.getTotalAmount()).isCloseTo(44.48, org.assertj.core.data.Offset.offset(0.01));
                    assertThat(response.getSpecialInstructions()).isEqualTo("Please deliver after 7 PM");
                    assertThat(response.getPriority()).isEqualTo(OrderPriority.NORMAL);
                    createdOrderId = response.getOrderId();
                });
    }

    @Test
    @Order(2)
    void shouldGetOrderById() {
        // First create an order
        CreateOrderRequest request = CreateOrderRequest.builder()
                .travelerId("TRV-002")
                .travelerName("Jane Smith")
                .propertyId("PROP-MARRIOTT-LA-001")
                .roomNumber("503")
                .orderType(OrderType.SPA_WELLNESS)
                .items(List.of(
                        OrderItemRequest.builder()
                                .itemId("SPA-001")
                                .itemName("Deep Tissue Massage")
                                .quantity(1)
                                .unitPrice(120.00)
                                .build()
                ))
                .priority(OrderPriority.HIGH)
                .build();

        OrderResponse created = webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();

        webTestClient.get()
                .uri("/api/v1/orders/{orderId}", created.getOrderId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.getOrderId()).isEqualTo(created.getOrderId());
                    assertThat(response.getTravelerId()).isEqualTo("TRV-002");
                    assertThat(response.getOrderType()).isEqualTo(OrderType.SPA_WELLNESS);
                });
    }

    @Test
    @Order(3)
    void shouldGetAllOrders() {
        webTestClient.get()
                .uri("/api/v1/orders")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OrderResponse.class)
                .value(orders -> assertThat(orders).isNotEmpty());
    }

    @Test
    @Order(4)
    void shouldModifyOrder() {
        // Create an order first
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .travelerId("TRV-003")
                .travelerName("Bob Wilson")
                .propertyId("PROP-HYATT-CHI-001")
                .roomNumber("801")
                .orderType(OrderType.RESTAURANT_DINING)
                .items(List.of(
                        OrderItemRequest.builder()
                                .itemId("FOOD-001")
                                .itemName("Steak")
                                .quantity(1)
                                .unitPrice(45.00)
                                .build()
                ))
                .priority(OrderPriority.NORMAL)
                .build();

        OrderResponse created = webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();

        // Modify the order
        ModifyOrderRequest modifyRequest = ModifyOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder()
                                .itemId("FOOD-001")
                                .itemName("Steak")
                                .quantity(2)
                                .unitPrice(45.00)
                                .build(),
                        OrderItemRequest.builder()
                                .itemId("FOOD-002")
                                .itemName("Lobster Bisque")
                                .quantity(1)
                                .unitPrice(18.00)
                                .build()
                ))
                .specialInstructions("Medium rare steak")
                .priority(OrderPriority.HIGH)
                .build();

        webTestClient.put()
                .uri("/api/v1/orders/{orderId}", created.getOrderId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(modifyRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.getItems()).hasSize(2);
                    assertThat(response.getTotalAmount()).isEqualTo(108.00);
                    assertThat(response.getSpecialInstructions()).isEqualTo("Medium rare steak");
                    assertThat(response.getPriority()).isEqualTo(OrderPriority.HIGH);
                    assertThat(response.getStatus()).isEqualTo(OrderStatus.MODIFIED);
                });
    }

    @Test
    @Order(5)
    void shouldCancelOrder() {
        // Create an order first
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .travelerId("TRV-004")
                .travelerName("Alice Brown")
                .propertyId("PROP-HILTON-NYC-001")
                .roomNumber("305")
                .orderType(OrderType.LAUNDRY)
                .items(List.of(
                        OrderItemRequest.builder()
                                .itemId("LAUNDRY-001")
                                .itemName("Express Laundry Service")
                                .quantity(1)
                                .unitPrice(25.00)
                                .build()
                ))
                .priority(OrderPriority.LOW)
                .build();

        OrderResponse created = webTestClient.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();

        // Cancel the order
        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
                .reason("Changed plans")
                .build();

        webTestClient.post()
                .uri("/api/v1/orders/{orderId}/cancel", created.getOrderId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cancelRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                    assertThat(response.getCancellationReason()).isEqualTo("Changed plans");
                });
    }

    @Test
    @Order(6)
    void shouldReturnNotFoundForNonExistentOrder() {
        webTestClient.get()
                .uri("/api/v1/orders/{orderId}", "non-existent-id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(7)
    void shouldReturnHealthCheck() {
        webTestClient.get()
                .uri("/api/v1/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.service").isEqualTo("order-orchestrator");
    }
}
