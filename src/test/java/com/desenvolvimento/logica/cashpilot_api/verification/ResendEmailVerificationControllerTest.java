package com.desenvolvimento.logica.cashpilot_api.verification;

import com.desenvolvimento.logica.cashpilot_api.verification.controller.ResendEmailVerificationController;
import com.desenvolvimento.logica.cashpilot_api.verification.dto.ResendVerificationRequest;
import com.desenvolvimento.logica.cashpilot_api.verification.service.ResendEmailVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResendEmailVerificationController.class)
class ResendEmailVerificationControllerTest {

    private static final String URL = "/api/auth/resend-verification";

    private static final String GENERIC_MESSAGE =
            "Se houver uma conta elegível para esse endereço, "
                    + "enviaremos as instruções de confirmação.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResendEmailVerificationService resendService;

    @Test
    void shouldNormalizeEmailAndReturnGenericResponse() throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "  Usuario@Example.COM  "
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.message").value(GENERIC_MESSAGE))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        verify(resendService).resend(
                new ResendVerificationRequest("usuario@example.com")
        );

        verifyNoMoreInteractions(resendService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sem-arroba", "usuario@@example.com"})
    void shouldRejectInvalidEmail(String email) throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email": "%s"}
                                        """.formatted(email))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(resendService);
    }

    @Test
    void shouldRejectMissingEmail() throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(resendService);
    }

    @Test
    void shouldRejectMissingBody() throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(resendService);
    }
}