package edu.java.configuration;

import edu.java.service.GitHubClient;
import edu.java.service.StackOverflowClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfiguration {
    private final ApplicationConfig applicationConfig;

    @Autowired
    public ClientConfiguration(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    @SuppressWarnings("MagicNumber")
    @Bean
    @Scope("prototype")
    WebClient webClient(String baseUrl) {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .exchangeStrategies(ExchangeStrategies
                .builder()
                .codecs(codecs -> codecs
                    .defaultCodecs()
                    .maxInMemorySize(500 * 1024))
                .build())
            .build();
    }

    @Bean
    StackOverflowClient stackOverflowClient() {
        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient(applicationConfig.baseUrlStackOverflow())))
                .build();
        return httpServiceProxyFactory.createClient(StackOverflowClient.class);
    }

    @Bean
    GitHubClient gitHubClient() {
        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient(applicationConfig.baseUrlGitHub())))
                .build();
        return httpServiceProxyFactory
            .createClient(GitHubClient.class);
    }
}