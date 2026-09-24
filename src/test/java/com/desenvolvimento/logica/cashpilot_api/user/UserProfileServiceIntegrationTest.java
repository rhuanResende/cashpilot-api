package com.desenvolvimento.logica.cashpilot_api.user;

import com.desenvolvimento.logica.cashpilot_api.shared.dto.AddressRequest;
import com.desenvolvimento.logica.cashpilot_api.user.dto.UpdateUserProfileRequest;
import com.desenvolvimento.logica.cashpilot_api.user.entity.PlatformRole;
import com.desenvolvimento.logica.cashpilot_api.user.entity.User;
import com.desenvolvimento.logica.cashpilot_api.user.entity.UserStatus;
import com.desenvolvimento.logica.cashpilot_api.user.repository.UserRepository;
import com.desenvolvimento.logica.cashpilot_api.user.service.UserProfileService;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public class UserProfileServiceIntegrationTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private UserProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldUpdateProfileAndPreserveAuthenticationData() {
        User user = createUser();
        UUID userId = user.getId();
        String originalEmail = user.getEmail();
        String originalHash = user.getPasswordHash();

        var address = new AddressRequest(
                "01310-100",
                "Avenida de teste",
                "123A",
                "Sala 10",
                "Bairro de teste",
                "São Paulo",
                "sp",
                "br"
        );

        var request = new UpdateUserProfileRequest(
                "  Nome atualizado  ",
                "529.982.247-25",
                "+55 (11) 99999-9999",
                address
        );

        profileService.updateProfile(userId, request);

        entityManager.flush();
        entityManager.clear();

        User updated = userRepository.findById(userId).orElseThrow();

        assertThat(updated.getName()).isEqualTo("Nome atualizado");
        assertThat(updated.getDocument()).isEqualTo("52998224725");
        assertThat(updated.getPhone()).isEqualTo("+5511999999999");

        assertThat(updated.getAddress()).isNotNull();
        assertThat(updated.getAddress().getPostalCode()).isEqualTo("01310100");
        assertThat(updated.getAddress().getStreet()).isEqualTo("Avenida de teste");
        assertThat(updated.getAddress().getNumber()).isEqualTo("123A");
        assertThat(updated.getAddress().getComplement()).isEqualTo("Sala 10");
        assertThat(updated.getAddress().getNeighborhood()).isEqualTo("Bairro de teste");
        assertThat(updated.getAddress().getCity()).isEqualTo("São Paulo");
        assertThat(updated.getAddress().getState()).isEqualTo("SP");
        assertThat(updated.getAddress().getCountryCode()).isEqualTo("BR");

        assertThat(updated.getEmail()).isEqualTo(originalEmail);
        assertThat(updated.getPasswordHash()).isEqualTo(originalHash);
        assertThat(updated.getPlatformRole()).isEqualTo(PlatformRole.USER);
        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updated.getEmailVerifiedAt()).isNull();
    }

    @Test
    void shouldRejectInvalidDocument() {
        User user = createUser();

        var request = new UpdateUserProfileRequest(
                "Nome atualizado",
                "11111111111",
                null,
                null
        );

        assertThatThrownBy(
                () -> profileService.updateProfile(user.getId(), request)
        ).isInstanceOf(ConstraintViolationException.class);

        assertThat(user.getName()).isEqualTo("Nome original");
        assertThat(user.getDocument()).isNull();
    }

    private User createUser() {
        User user = new User(
                "Nome original",
                "perfil-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("Uma senha longa para teste!")
        );

        userRepository.saveAndFlush(user);
        return user;
    }
}
