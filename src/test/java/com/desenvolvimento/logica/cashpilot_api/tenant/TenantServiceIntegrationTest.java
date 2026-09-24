package com.desenvolvimento.logica.cashpilot_api.tenant;

import com.desenvolvimento.logica.cashpilot_api.tenant.entity.Tenant;
import com.desenvolvimento.logica.cashpilot_api.tenant.entity.TenantStatus;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import com.desenvolvimento.logica.cashpilot_api.tenant.service.TenantService;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public class TenantServiceIntegrationTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistTenantWithGeneratedIdStatusAndTimestamps() {
        Tenant created = tenantService.create("Organização de teste");

        // Executa o INSERT e remove os objetos do cache do JPA.
        entityManager.flush();
        var id = created.getId();
        entityManager.clear();

        Assertions.assertThat(id).isNotNull();

        Tenant persisted = tenantRepository.findById(id)
                .orElseThrow();

        Assertions.assertThat(persisted.getName())
                .isEqualTo("Organização de teste");
        Assertions.assertThat(persisted.getStatus())
                .isEqualTo(TenantStatus.ACTIVE);
        Assertions.assertThat(persisted.getCreatedAt()).isNotNull();
        Assertions.assertThat(persisted.getUpdatedAt())
                .isEqualTo(persisted.getCreatedAt());
    }
}
