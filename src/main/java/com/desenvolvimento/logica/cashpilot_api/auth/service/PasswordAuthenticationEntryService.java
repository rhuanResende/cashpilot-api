package com.desenvolvimento.logica.cashpilot_api.auth.service;

import com.desenvolvimento.logica.cashpilot_api.auth.dto.AuthenticationRequestContext;
import com.desenvolvimento.logica.cashpilot_api.auth.dto.LoginRateLimitResult;
import com.desenvolvimento.logica.cashpilot_api.auth.dto.PasswordAuthenticationResult;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventOutcome;
import com.desenvolvimento.logica.cashpilot_api.auth.model.AuthEventType;
import com.desenvolvimento.logica.cashpilot_api.auth.web.ClientIpResolver;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.LoginRateLimitUnavailableException;
import com.desenvolvimento.logica.cashpilot_api.shared.exception.TooManyLoginAttemptsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
public class PasswordAuthenticationEntryService {

    private final ClientIpResolver ipResolver;
    private final LoginIpRateLimiter rateLimiter;
    private final PasswordAuthenticationService authenticationService;
    private final AuthAuditService auditService;
    private final TransactionTemplate auditTransaction;

    public PasswordAuthenticationEntryService(
            ClientIpResolver ipResolver,
            LoginIpRateLimiter rateLimiter,
            PasswordAuthenticationService authenticationService,
            AuthAuditService auditService,
            PlatformTransactionManager transactionManager
    ) {
        this.ipResolver = ipResolver;
        this.rateLimiter = rateLimiter;
        this.authenticationService = authenticationService;
        this.auditService = auditService;
        this.auditTransaction = new TransactionTemplate(transactionManager);
    }

    @Transactional(propagation = Propagation.NEVER)
    public PasswordAuthenticationResult authenticate(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password,
            @NotNull HttpServletRequest request
    ) {
        var address = ipResolver.resolve(request);

        var context = new AuthenticationRequestContext(
                UUID.randomUUID(),
                address.getHostAddress(),
                request.getHeader("User-Agent")
        );

        LoginRateLimitResult rateLimit;

        try {
            rateLimit = rateLimiter.consume(address);
        } catch (LoginRateLimitUnavailableException exception) {
            recordDenial("RATE_LIMIT_UNAVAILABLE", context);
            throw exception;
        }

        if (!rateLimit.allowed()) {
            recordDenial("IP_RATE_LIMIT_EXCEEDED", context);

            throw new TooManyLoginAttemptsException(
                    rateLimit.retryAfter()
            );
        }

        return authenticationService.authenticate(
                email,
                password,
                context
        );
    }

    private void recordDenial(
            String reasonCode,
            AuthenticationRequestContext context
    ) {
        auditTransaction.executeWithoutResult(status ->
                auditService.record(
                        AuthEventType.ACCESS_DENIED,
                        AuthEventOutcome.DENIED,
                        reasonCode,
                        null,
                        null,
                        null,
                        context.sourceIp(),
                        context.userAgent(),
                        context.requestId()
                )
        );
    }
}
