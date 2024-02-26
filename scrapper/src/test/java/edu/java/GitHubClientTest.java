package edu.java;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.java.service.GitHubClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class GitHubClientTest {
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(wireMockConfig().port(8080))
        .build();
    WebClient webClient = WebClient.builder()
        .baseUrl("http://localhost:8080")
        .exchangeStrategies(ExchangeStrategies
            .builder()
            .codecs(codecs -> codecs
                .defaultCodecs()
                .maxInMemorySize(500 * 1024))
            .build())
        .build();
    HttpServiceProxyFactory httpServiceProxyFactory =
        HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(webClient))
            .build();

    @Test
    public void properServerResponse() {
        var gh = httpServiceProxyFactory.createClient(GitHubClient.class);
        wm.stubFor(get(urlPathMatching("/repos/Marijarin/tocook/events"))
            .willReturn(aResponse()
                .withStatus(200)));
        try {
            gh.getResponse("Marijarin", "tocook");
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        wm.verify(getRequestedFor(urlPathMatching("/repos/.*")));
    }
}
