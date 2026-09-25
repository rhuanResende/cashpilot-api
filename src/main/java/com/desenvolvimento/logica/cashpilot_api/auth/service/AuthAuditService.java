package com.desenvolvimento.logica.cashpilot_api.auth.service;

import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEvent;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventOutcome;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventType;
import com.desenvolvimento.logica.cashpilot_api.auth.repository.AuthEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthAuditService {

    private final AuthEventRepository repository;
    private final Clock clock;

    public AuthAuditService(
            AuthEventRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            AuthEventType eventType,
            AuthEventOutcome outcome,
            String reasonCode,
            UUID userId,
            UUID actorUserId,
            UUID tenantId,
            String sourceIp,
            String userAgent,
            UUID requestId
    ) {
        AuthEvent event = new AuthEvent(
                Instant.now(clock),
                eventType,
                outcome,
                reasonCode,
                userId,
                actorUserId,
                tenantId,
                sourceIp,
                userAgent,
                requestId
        );

        repository.save(event);
    }
}
