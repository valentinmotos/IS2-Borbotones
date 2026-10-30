package com.davinchicoder.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLoggingFilterTests {
    @Test
    void forwardsRequestsWithoutSecurityContext() {
        AtomicBoolean forwarded = new AtomicBoolean();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/employees"));
        new RequestLoggingFilter().filter(exchange, request -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();
        assertTrue(forwarded.get());
    }
}
