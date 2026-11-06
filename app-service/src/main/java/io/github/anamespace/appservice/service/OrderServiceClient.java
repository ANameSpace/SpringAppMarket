package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Integration point with order-service.
 *
 * <p>The real order-service is not implemented yet, therefore the demo build
 * treats every authenticated non-owner as a buyer and turns the two
 * orchestration calls into no-ops. The methods already have their final shape,
 * so replacing the stub with a WebClient later does not affect the rest of the
 * app-service.</p>
 */
@Component
public class OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);

    private final AppProperties properties;

    public OrderServiceClient(AppProperties properties) {
        this.properties = properties;
    }

    public boolean hasCompletedPurchase(Long userId, UUID appId) {
        if (properties.getIntegrations().isStubPurchaseCheck()) {
            return true;
        }
        // TODO: GET {order-service}/internal/orders/completed?userId=&appId=
        return true;
    }

    public void replaceApp(UUID fromAppId, UUID toAppId) {
        // TODO: POST {order-service}/internal/orders/replace-app
        log.info("Stub: replace app {} with {} in order items", fromAppId, toAppId);
    }

    public void deleteCartByApp(UUID appId) {
        // TODO: DELETE {order-service}/internal/cart/by-app/{appId}
        log.info("Stub: delete cart items for app {}", appId);
    }
}
