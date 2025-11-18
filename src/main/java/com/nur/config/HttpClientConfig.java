package com.nur.config;

import com.nur.config.properties.OAuthProps;
import lombok.RequiredArgsConstructor;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;

@Configuration
@RequiredArgsConstructor
public class HttpClientConfig {

    private final OAuthProps oAuthProps;

    @Bean
    public RestTemplate oauthRestTemplate() throws Exception {

        try (var in = new FileInputStream(Path.of(oAuthProps.trustStorePath()).toFile())) {
            var trustStore = KeyStore.getInstance(oAuthProps.trustStoreType());
            trustStore.load(in, oAuthProps.trustStorePassword().toCharArray());

            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null)
                    .build();

            var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .build())
                    .build();

            HttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .evictExpiredConnections()
                    .build();

            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        }
    }
}