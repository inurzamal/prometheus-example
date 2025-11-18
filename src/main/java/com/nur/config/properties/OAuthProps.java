package com.nur.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.custom")
public record OAuthProps(
        String clientId,
        String clientSecret,
        String scope,
        String baseUrl,
        String realmPublicKey,
        String getRealmPublicKeyAlgorithm,
        String getRealmPublicKeyUrl,
        String trustStorePath,
        String trustStorePassword,
        String trustStoreType
) {
}
