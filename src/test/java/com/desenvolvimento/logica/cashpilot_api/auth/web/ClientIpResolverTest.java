package com.desenvolvimento.logica.cashpilot_api.auth.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void shouldResolveIpv4() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");

        var result = resolver.resolve(request);

        assertThat(result.getHostAddress()).isEqualTo("192.0.2.10");
    }

    @Test
    void shouldResolveIpv6() throws Exception {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("2001:db8::10");

        var result = resolver.resolve(request);

        assertThat(result.getAddress()).containsExactly(
                InetAddress.getByName("2001:db8::10").getAddress()
        );
    }

    @Test
    void shouldTreatEquivalentIpv6RepresentationsAsSameAddress() {
        var compressed = new MockHttpServletRequest();
        compressed.setRemoteAddr("2001:db8::10");

        var expanded = new MockHttpServletRequest();
        expanded.setRemoteAddr("2001:0db8:0000:0000:0000:0000:0000:0010");

        assertThat(resolver.resolve(compressed).getAddress())
                .containsExactly(resolver.resolve(expanded).getAddress());
    }

    @Test
    void shouldIgnoreForwardedHeaders() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");

        request.addHeader("X-Forwarded-For", "198.51.100.20");
        request.addHeader("X-Real-IP", "203.0.113.30");
        request.addHeader("Forwarded", "for=198.51.100.40");

        var result = resolver.resolve(request);

        assertThat(result.getHostAddress()).isEqualTo("192.0.2.10");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "localhost",
            "example.com",
            "999.0.2.10",
            "192.0.2",
            "192.0.2.010",
            "192.0.2.10:8080",
            "192.0.2.10,198.51.100.20",
            "2001:db8:::10",
            "[2001:db8::10]",
            "fe80::1%eth0"
    })
    void shouldRejectInvalidOrUnsupportedAddress(String address) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(address);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Não foi possível identificar um IP válido da conexão"
                );
    }

    @Test
    void shouldNotUseForwardedHeaderWhenRemoteAddressIsInvalid() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("");
        request.addHeader("X-Forwarded-For", "192.0.2.10");

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(IllegalStateException.class);
    }
}