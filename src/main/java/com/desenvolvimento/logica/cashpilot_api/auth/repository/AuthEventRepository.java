package com.desenvolvimento.logica.cashpilot_api.auth.repository;

import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEvent;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface AuthEventRepository
        extends Repository<AuthEvent, UUID> {

    AuthEvent save(AuthEvent event);

    List<AuthEvent> findAllByRequestIdOrderByOccurredAtAsc(UUID requestId);
}
