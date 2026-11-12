package br.com.senai.tv_score.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        return new RestTemplate(factory);
    }
    @Bean("recomendadorHttp")
    public RestTemplate recomendadorHttp(@org.springframework.beans.factory.annotation.Value("${recomendacao.api.timeout-ms:60000}") int timeout) {
        if (timeout < 1) throw new IllegalArgumentException("Timeout deve ser positivo.");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
}
