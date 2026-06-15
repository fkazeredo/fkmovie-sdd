package com.fksoft.infra.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.dao.OptimisticLockingFailureException;

/** A concurrency conflict maps to the clean {@code concurrent.conflict} contract, not a raw 500. */
class GlobalExceptionHandlerTest {

    private final MessageSource messages = mock(MessageSource.class);
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messages);

    @Test
    void optimisticLockBecomesConcurrentConflict() {
        when(messages.getMessage(eq("concurrent.conflict"), any(), any())).thenReturn("Conflict, retry.");

        var body = handler.handleConcurrentConflict(new OptimisticLockingFailureException("version clash"));

        assertThat(body.code()).isEqualTo("concurrent.conflict");
        assertThat(body.message()).isEqualTo("Conflict, retry.");
    }
}
