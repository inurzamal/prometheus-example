package com.nur.config;

import com.nur.config.properties.OAuthProps;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
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
        // Load truststore
        try (var in = new FileInputStream(Path.of(oAuthProps.trustStorePath()).toFile())) {
            KeyStore trustStore = KeyStore.getInstance(oAuthProps.trustStoreType());
            trustStore.load(in, oAuthProps.trustStorePassword().toCharArray());

            // Create SSLContext
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(trustStore, null)
                    .build();

            // Create a TlsSocketStrategy for classic blocking IO
            TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);

            // Build connection manager with TLS strategy
            PoolingHttpClientConnectionManager connectionManager =
                    PoolingHttpClientConnectionManagerBuilder.create()
                            .setTlsSocketStrategy(tlsStrategy)
                            // optionally configure max connections etc:
                            // .setMaxConnTotal(100)
                            // .setMaxConnPerRoute(20)
                            .build();

            // Build HttpClient
            HttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .evictExpiredConnections() // optional
                    .build();

            // Use HttpClient in RestTemplate
            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        }
    }
}
