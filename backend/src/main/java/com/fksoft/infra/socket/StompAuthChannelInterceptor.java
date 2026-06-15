package com.fksoft.infra.socket;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Authenticates the STOMP CONNECT frame (SPEC-0013, ADR 0009): viewing the seat map over REST is
 * public, but the live channel requires {@code Authorization: Bearer <jwt>}. Validates with the same
 * {@code JwtDecoder} as the REST chain and binds the user (subject) so {@code /user/**} destinations
 * route correctly. An invalid or missing token rejects the connection.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final JwtDecoder jwtDecoder;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeConnections = new AtomicInteger();

    StompAuthChannelInterceptor(JwtDecoder jwtDecoder, MeterRegistry meterRegistry) {
        this.jwtDecoder = jwtDecoder;
        this.meterRegistry = meterRegistry;
        Gauge.builder("ws_connections_active", activeConnections, AtomicInteger::get)
                .register(meterRegistry);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            activeConnections.incrementAndGet();
        }
        return message;
    }

    @EventListener
    void onDisconnect(SessionDisconnectEvent event) {
        activeConnections.decrementAndGet();
    }

    private StompPrincipal authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw rejected("missing bearer token");
        }
        try {
            var jwt = jwtDecoder.decode(authorizationHeader.substring("Bearer ".length()));
            return new StompPrincipal(jwt.getSubject());
        } catch (JwtException ex) {
            throw rejected("invalid token");
        }
    }

    private MessageDeliveryException rejected(String reason) {
        meterRegistry.counter("ws_connect_rejected_total").increment();
        log.info("ws connect rejected: {}", reason);
        return new MessageDeliveryException(reason);
    }
}
