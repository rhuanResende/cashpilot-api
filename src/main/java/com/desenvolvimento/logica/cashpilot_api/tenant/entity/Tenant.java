package com.desenvolvimento.logica.cashpilot_api.tenant.entity;

import com.desenvolvimento.logica.cashpilot_api.shared.model.Address;
import com.desenvolvimento.logica.cashpilot_api.shared.model.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 10)
    private DocumentType documentType;

    @Column(name = "document", length = 14)
    private String document;

    @Column(name = "contact_email", length = 254)
    private String contactEmail;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Embedded
    private Address address;

    protected Tenant() {
    }

    public Tenant(String name) {
        this.name = name;
    }

    public void rename(String name) {
        this.name = validateName(name);
    }

    public void inactivate() {
        this.status = TenantStatus.INACTIVE;
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    public void activate() {
        this.status = TenantStatus.ACTIVE;
    }

    public void updateProfile(
            String name,
            String legalName,
            DocumentType documentType,
            String document,
            String contactEmail,
            String phone,
            Address address) {

        if ((documentType == null) != (document == null)) {
            throw new IllegalArgumentException(
                    "Informe o documento e seu tipo juntos."
            );
        }

        this.name = validateName(name);
        this.legalName = legalName;
        this.documentType = documentType;
        this.document = document;
        this.contactEmail = contactEmail;
        this.phone = phone;
        this.address = address;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome da organização é obrigatório."
            );
        }

        String normalized = name.strip();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "O nome da organização deve ter até 150 caracteres."
            );
        }

        return normalized;
    }

    public String getName() {
        return name;
    }

    public String getLegalName() {
        return legalName;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocument() {
        return document;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getPhone() {
        return phone;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public Address getAddress() {
        return address;
    }
}
