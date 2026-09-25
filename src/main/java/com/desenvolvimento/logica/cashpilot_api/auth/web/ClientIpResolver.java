package com.desenvolvimento.logica.cashpilot_api.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile(
            "(0|[1-9][0-9]{0,2})(\\.(0|[1-9][0-9]{0,2})){3}"
    );

    private static final Pattern IPV6_CHARACTERS = Pattern.compile(
            "[0-9a-fA-F:.]+"
    );

    public InetAddress resolve(HttpServletRequest request) {
        Objects.requireNonNull(request, "A requisição é obrigatória");

        String remoteAddress = request.getRemoteAddr();

        if (remoteAddress == null
                || remoteAddress.isBlank()
                || remoteAddress.length() > 45) {
            throw invalidAddress();
        }

        try {
            if (IPV4.matcher(remoteAddress).matches()) {
                return parseIpv4(remoteAddress);
            }

            if (remoteAddress.indexOf(':') >= 0
                    && IPV6_CHARACTERS.matcher(remoteAddress).matches()) {
                return InetAddress.getByName(remoteAddress);
            }
        } catch (UnknownHostException exception) {
            throw invalidAddress();
        }

        throw invalidAddress();
    }

    private InetAddress parseIpv4(String address)
            throws UnknownHostException {
        String[] parts = address.split("\\.");
        byte[] bytes = new byte[4];

        for (int i = 0; i < parts.length; i++) {
            int value = Integer.parseInt(parts[i]);

            if (value > 255) {
                throw invalidAddress();
            }

            bytes[i] = (byte) value;
        }

        return InetAddress.getByAddress(bytes);
    }

    private IllegalStateException invalidAddress() {
        return new IllegalStateException(
                "Não foi possível identificar um IP válido da conexão"
        );
    }
}
