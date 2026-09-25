package com.desenvolvimento.logica.cashpilot_api.tenant.service;

import com.desenvolvimento.logica.cashpilot_api.tenant.model.Tenant;
import com.desenvolvimento.logica.cashpilot_api.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Tenant create(String name) {
        Tenant tenant = new Tenant(name);
        return tenantRepository.save(tenant);
    }
}
