package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.entities.properties.CartValidation.CartValidationType;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.cart_service.exceptions.InvalidCartUpdateRequestVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class CartUpdateRequestHandlerTests {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4.2-alpine").withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.4"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.connect-timeout", () -> 5);

        registry.add("spring.kafka.admin.auto-create", () -> true);
        registry.add("spring.kafka.listener.auto-startup", () -> true);
        registry.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
    }

    @Autowired
    private CartUpdateRequestHandler handler;

    @Autowired
    private LockRepository lockRepo;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private Map<String, ProductAvailability> productAvailabilities;

    private CartUpdateRequest buildRequest(String usrId) {
        return new CartUpdateRequest(usrId, 1, List.of(
            new CartUpdateRequest.CartUpdateRequestEntry("PRODUCT_001", "Product_1", 2, CartUpdateRequest.CartAction.QTY_CHANGE),
            new CartUpdateRequest.CartUpdateRequestEntry("PRODUCT_002", "Product_2", 3, CartUpdateRequest.CartAction.QTY_CHANGE)
        ), "unknown");
    }

    @Test
    void whenLockUnavailable_thenCommitRequestWithoutSave() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);
        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        var acquireLockAndCallHandler = lockRepo
            .acquireLock("carts:"+ userId, "1", Duration.ofSeconds(3))
            .then(handler.handle(request, () -> commited.set(true), () -> saved.set(true)))
        ;

        assertThrows(LockUnavailable.class, acquireLockAndCallHandler::block);
        assertTrue(commited.get());
        assertFalse(saved.get());
    }

    @Test
    void whenRequestVersionMismatch_thenCommitRequestWithoutSave() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);
        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        var callHandler = handler.handle(request, () -> commited.set(true), () -> saved.set(true));
        request.setVersionNumber(-1);

        assertThrows(InvalidCartUpdateRequestVersion.class, callHandler::block);
        assertTrue(commited.get());
        assertFalse(saved.get());
    }

    @Test
    void whenCartNotExist_thenPutNewCart() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);
        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        var cartAfterSaved = handler
            .handle(request, () -> commited.set(true), () -> saved.set(true))
            .then(cartRepo.getCartByUserId(userId))
            .block();

        assertTrue(commited.get());
        assertTrue(saved.get());
        assertNotNull(cartAfterSaved);
        assertEquals(userId, cartAfterSaved.getUserId());
        assertEquals(
            Map.of(
                "prod_1_id", request.getEntries().get(0).getProductId(),
                "prod_1_name", request.getEntries().get(0).getProductName(),
                "prod_1_qty", request.getEntries().get(0).getQtyAdjustment(),

                "prod_2_id", request.getEntries().get(1).getProductId(),
                "prod_2_name", request.getEntries().get(1).getProductName(),
                "prod_2_qty", request.getEntries().get(1).getQtyAdjustment()
            ),
            Map.of(
                "prod_1_id", cartAfterSaved.getItems().get("PRODUCT_001").getProductId(),
                "prod_1_name", cartAfterSaved.getItems().get("PRODUCT_001").getProductName(),
                "prod_1_qty", cartAfterSaved.getItems().get("PRODUCT_001").getQuantity(),

                "prod_2_id", cartAfterSaved.getItems().get("PRODUCT_002").getProductId(),
                "prod_2_name", cartAfterSaved.getItems().get("PRODUCT_002").getProductName(),
                "prod_2_qty", cartAfterSaved.getItems().get("PRODUCT_002").getQuantity()
            )
        );
    }

    @Test
    void whenCartExists_thenUpdateCart() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);
        var userId = UUID.randomUUID().toString();
        var cart = new Cart(userId);
        var request = buildRequest(userId);

        cart.setProductAvailabilities(productAvailabilities);
        cart.setVersionNumber(1);
        request.setVersionNumber(2);

        var saveCartThenRequestUpdate = cartRepo
            .saveCart(cart)
            .then(handler.handle(request, () -> commited.set(true), () -> saved.set(true)))
            .then(cartRepo.getCartByUserId(userId))
        ;
        var cartAfterSaved = saveCartThenRequestUpdate.block();

        assertTrue(commited.get());
        assertTrue(saved.get());
        assertNotNull(cartAfterSaved);
        assertEquals(userId, cartAfterSaved.getUserId());
        assertEquals(
            Map.of(
                "prod_1_id", request.getEntries().get(0).getProductId(),
                "prod_1_name", request.getEntries().get(0).getProductName(),
                "prod_1_qty", request.getEntries().get(0).getQtyAdjustment(),

                "prod_2_id", request.getEntries().get(1).getProductId(),
                "prod_2_name", request.getEntries().get(1).getProductName(),
                "prod_2_qty", request.getEntries().get(1).getQtyAdjustment()
            ),
            Map.of(
                "prod_1_id", cartAfterSaved.getItems().get("PRODUCT_001").getProductId(),
                "prod_1_name", cartAfterSaved.getItems().get("PRODUCT_001").getProductName(),
                "prod_1_qty", cartAfterSaved.getItems().get("PRODUCT_001").getQuantity(),

                "prod_2_id", cartAfterSaved.getItems().get("PRODUCT_002").getProductId(),
                "prod_2_name", cartAfterSaved.getItems().get("PRODUCT_002").getProductName(),
                "prod_2_qty", cartAfterSaved.getItems().get("PRODUCT_002").getQuantity()
            )
        );
    }

    @Test
    void afterSavedCart_alsoSetValidations() {
        var userId = UUID.randomUUID().toString();
        var cart = new Cart(userId);
        var request = buildRequest(userId);

        cart.setProductAvailabilities(productAvailabilities);
        cart.setVersionNumber(1);
        request.setVersionNumber(2);

        var saveCartThenRequestUpdate = cartRepo
            .saveCart(cart)
            .then(handler.handle(request, null, null))
            .then(cartRepo.getCartByUserId(userId))
        ;
        var saved = saveCartThenRequestUpdate.block();

        assertNotNull(saved);
        assertEquals(CartValidationType.PRODUCT_UNAVAILABLE, saved.getValidations().get("PRODUCT_001").getType());
        assertEquals(CartValidationType.OUT_OF_STOCK, saved.getValidations().get("PRODUCT_002").getType());
    }
}
