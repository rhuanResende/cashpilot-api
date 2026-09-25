package com.desenvolvimento.logica.cashpilot_api.membership.model;

import com.desenvolvimento.logica.cashpilot_api.shared.model.BaseEntity;
import com.desenvolvimento.logica.cashpilot_api.tenant.model.Tenant;
import com.desenvolvimento.logica.cashpilot_api.user.model.User;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(
        name = "tenant_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tenant_memberships_tenant_user",
                columnNames = {"tenant_id", "user_id"}
        )
)
public class TenantMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    protected TenantMembership() {
    }

    public TenantMembership(
            Tenant tenant,
            User user,
            MembershipRole role) {

        this.tenant = Objects.requireNonNull(
                tenant, "A organização é obrigatória."
        );
        this.user = Objects.requireNonNull(
                user, "O usuário é obrigatório."
        );
        this.role = Objects.requireNonNull(
                role, "O papel é obrigatório."
        );
    }

    public Tenant getTenant() {
        return tenant;
    }

    public User getUser() {
        return user;
    }

    public MembershipRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }
}
