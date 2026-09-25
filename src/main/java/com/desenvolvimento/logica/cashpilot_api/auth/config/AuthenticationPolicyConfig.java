package com.desenvolvimento.logica.cashpilot_api.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationPolicyProperties.class)
public class AuthenticationPolicyConfig {
}
