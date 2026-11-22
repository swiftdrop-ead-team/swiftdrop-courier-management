package com.swiftdrop.gateway;

import com.swiftdrop.gateway.filter.AuthenticationFilter;
import com.swiftdrop.gateway.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AuthenticationFilterTest {
    private final JwtUtil jwt = mock(JwtUtil.class);
    private AuthenticationFilter filter() { return new AuthenticationFilter(jwt); }
    @Test void delegatesMerchantKeysToMerchantServiceOnly() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/merchant/orders").header("X-API-Key","test-key"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class); when(chain.filter(exchange)).thenReturn(Mono.empty());
        filter().apply(new AuthenticationFilter.Config()).filter(exchange,chain).block(); verify(chain).filter(exchange);
        var privateParcel = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/parcels/private-id").header("X-API-Key","test-key"));
        filter().apply(new AuthenticationFilter.Config()).filter(privateParcel,chain).block();
        assertThat(privateParcel.getResponse().getStatusCode().value()).isEqualTo(401);
    }
    @Test void permitsOnlyPublicParcelTrackingGetWithoutJwt() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/parcels/tracking/SD-1234-LK"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class); when(chain.filter(exchange)).thenReturn(Mono.empty());
        filter().apply(new AuthenticationFilter.Config()).filter(exchange,chain).block(); verify(chain).filter(exchange);
        var mutation = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/parcels/tracking/SD-1234-LK"));
        filter().apply(new AuthenticationFilter.Config()).filter(mutation,chain).block();
        assertThat(mutation.getResponse().getStatusCode().value()).isEqualTo(401);
    }
    @Test void validatesBearerTokensAndRejectsInvalidSignatures() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/parcels").header("Authorization","Bearer invalid"));
        doThrow(new IllegalArgumentException()).when(jwt).validateToken("invalid");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        filter().apply(new AuthenticationFilter.Config()).filter(exchange,chain).block();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);verifyNoInteractions(chain);
    }
}
