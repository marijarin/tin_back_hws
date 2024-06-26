package edu.java.hw7;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.java.bot.client.ScrapperClient;
import edu.java.bot.configuration.CustomRetry;
import edu.java.bot.controller.dto.ApiErrorResponse;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@WireMockTest
public class ScrapperClientTest {
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(wireMockConfig().port(8080))
        .build();
    WebClient webClient1 = WebClient.builder()
        .baseUrl("http://localhost:8080")
        .exchangeStrategies(ExchangeStrategies
            .builder()
            .codecs(codecs -> codecs
                .defaultCodecs()
                .maxInMemorySize(500 * 1024))
            .build())
        .build();

    ExchangeFilterFunction linear1() {
        return (request, next) -> next.exchange(request)
            .flatMap(clientResponse -> Mono.just(clientResponse)
                .filter(response -> clientResponse.statusCode().isSameCodeAs(HttpStatusCode.valueOf(400)))
                .flatMap(response -> clientResponse.toEntity(ApiErrorResponse.class))
                .flatMap(mono -> {
                    var body = Optional.ofNullable(mono.getBody())
                        .orElse(
                            new ApiErrorResponse(" ", "400", new IllegalStateException("hello"), "", new String[0]));
                    var e = body.exception();
                    return Mono.error(e);
                })
                .thenReturn(clientResponse))
            .filter(throwable -> throwable instanceof IndexOutOfBoundsException)
            .retryWhen(new CustomRetry(6, 2, Duration.ofSeconds(1)));
    }

    ExchangeFilterFunction linear() {
        return (request, next) -> next.exchange(request)
            .flatMap(clientResponse -> Mono.just(clientResponse)
                .filter(response -> clientResponse.statusCode().isSameCodeAs(HttpStatusCode.valueOf(400)))
                .flatMap(response -> clientResponse.toEntity(ApiErrorResponse.class))
                .flatMap(mono -> {
                    var body = Optional.ofNullable(mono.getBody())
                        .orElse(
                            new ApiErrorResponse(" ", "400", new IllegalStateException("hello"), "", new String[0]));
                    var e = body.exception();
                    return Mono.error(e);
                })
                .thenReturn(clientResponse))
            .filter(throwable -> throwable instanceof IllegalStateException)
            .retryWhen(new CustomRetry(6, 2, Duration.ofSeconds(1)));
    }

    ExchangeFilterFunction constant() {
        return (request, next) -> next.exchange(request)
            .flatMap(clientResponse -> Mono.just(clientResponse)
                .filter(response -> clientResponse.statusCode().isError())
                .flatMap(response -> clientResponse.toEntity(ApiErrorResponse.class))
                .flatMap(mono -> {
                    var body = Optional.ofNullable(mono.getBody())
                        .orElse(
                            new ApiErrorResponse(" ", "400", new RuntimeException(), "", new String[0]));
                    var e = body.exception();
                    return Mono.error(e);
                })
                .thenReturn(clientResponse))
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)));
    }

    ExchangeFilterFunction constantWithGoodErrorFilterInRetry() {
        return (request, next) -> next.exchange(request)
            .flatMap(clientResponse -> Mono.just(clientResponse)
                .filter(response -> clientResponse.statusCode().isError())
                .flatMap(response -> clientResponse.toEntity(ApiErrorResponse.class))
                .flatMap(mono -> {
                    var body = Optional.ofNullable(mono.getBody())
                        .orElse(
                            new ApiErrorResponse(" ", "400", new IndexOutOfBoundsException(), "", new String[0]));
                    var e = body.exception();
                    return Mono.error(e);
                })
                .thenReturn(clientResponse))
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof IndexOutOfBoundsException));
    }

    ExchangeFilterFunction constantWithBadErrorFilterInRetry() {
        return (request, next) -> next.exchange(request)
            .flatMap(clientResponse -> Mono.just(clientResponse)
                .filter(response -> clientResponse.statusCode().isError())
                .flatMap(response -> clientResponse.toEntity(ApiErrorResponse.class))
                .flatMap(mono -> {
                    var body = Optional.ofNullable(mono.getBody())
                        .orElse(
                            new ApiErrorResponse(
                                " ",
                                "400",
                                new DataIntegrityViolationException("hi there"),
                                "",
                                new String[0]
                            ));
                    var e = body.exception();
                    return Mono.error(e);
                })
                .thenReturn(clientResponse))
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                .filter(throwable -> throwable instanceof IndexOutOfBoundsException));
    }

    ExchangeFilterFunction exponential() {
        return (request, next) -> next.exchange(request)
            .flatMap(clientResponse -> Mono.just(clientResponse)
                .filter(response -> clientResponse.statusCode().isError())
                .flatMap(response -> clientResponse.toEntity(ApiErrorResponse.class))
                .flatMap(mono -> {
                    var body = Optional.ofNullable(mono.getBody())
                        .orElse(
                            new ApiErrorResponse(" ", "400", new RuntimeException(), "", new String[0]));
                    var e = body.exception();
                    return Mono.error(e);
                })
                .thenReturn(clientResponse))
            .retryWhen(Retry.backoff(4, Duration.ofSeconds(2)));
    }

    @Test
    public void retries6TimesWhenLinearAndProperFilter() {
        HttpExchangeAdapter wc = WebClientAdapter
            .create(webClient1
                .mutate()
                .filter(linear())
                .build());

        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(wc)
                .build();
        var client = httpServiceProxyFactory.createClient(ScrapperClient.class);
        wm.stubFor(get(urlPathMatching("/tg-chat/1000"))
            .willReturn(aResponse()
                .withStatus(400)));
        try {
            client.findChat(1000L);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        wm.verify(1 + 6, getRequestedFor(urlPathMatching("/tg-chat/1000")));
    }

    @Test
    public void retries0TimesWhenLinearAndNotSuitableFilter() {
        HttpExchangeAdapter wc = WebClientAdapter
            .create(webClient1
                .mutate()
                .filter(linear1())
                .build());

        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(wc)
                .build();
        var client = httpServiceProxyFactory.createClient(ScrapperClient.class);
        wm.stubFor(get(urlPathMatching("/tg-chat/1000"))
            .willReturn(aResponse()
                .withStatus(429)));
        try {
            client.findChat(1000L);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        wm.verify(1, getRequestedFor(urlPathMatching("/tg-chat/1000")));
    }

    @Test
    public void retries3TimesWhenConstant() {
        HttpExchangeAdapter wc = WebClientAdapter
            .create(webClient1
                .mutate()
                .filter(constant())
                .build());

        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(wc)
                .build();
        var client = httpServiceProxyFactory.createClient(ScrapperClient.class);
        wm.stubFor(get(urlPathMatching("/tg-chat/1000"))
            .willReturn(aResponse()
                .withStatus(400)));
        try {
            client.findChat(1000L);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        wm.verify(1 + 3, getRequestedFor(urlPathMatching("/tg-chat/1000")));
    }

    @Test
    public void retries4TimesWhenExp() {
        HttpExchangeAdapter wc = WebClientAdapter
            .create(webClient1
                .mutate()
                .filter(exponential())
                .build());

        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(wc)
                .build();
        var client = httpServiceProxyFactory.createClient(ScrapperClient.class);
        wm.stubFor(get(urlPathMatching("/tg-chat/1000"))
            .willReturn(aResponse()
                .withStatus(400)));
        try {
            client.findChat(1000L);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        wm.verify(1 + 4, getRequestedFor(urlPathMatching("/tg-chat/1000")));
    }

    @Test
    public void noRetriesWhenBadFilterInsideRetry() {
        HttpExchangeAdapter wc = WebClientAdapter
            .create(webClient1
                .mutate()
                .filter(constantWithBadErrorFilterInRetry())
                .build());

        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(wc)
                .build();
        var client = httpServiceProxyFactory.createClient(ScrapperClient.class);
        wm.stubFor(get(urlPathMatching("/tg-chat/1000"))
            .willReturn(aResponse()
                .withStatus(400)));
        try {
            client.findChat(1000L);
        } catch (DataIntegrityViolationException e) {
            System.out.println(e.getMessage());
        }
        wm.verify(1, getRequestedFor(urlPathMatching("/tg-chat/1000")));
    }

    @Test
    public void Retries3WhenNeededFilterInsideRetry() {
        HttpExchangeAdapter wc = WebClientAdapter
            .create(webClient1
                .mutate()
                .filter(constantWithGoodErrorFilterInRetry())
                .build());

        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(wc)
                .build();
        var client = httpServiceProxyFactory.createClient(ScrapperClient.class);
        wm.stubFor(get(urlPathMatching("/tg-chat/1000"))
            .willReturn(aResponse()
                .withStatus(400)));
        try {
            client.findChat(1000L);
        } catch (IndexOutOfBoundsException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        wm.verify(1 + 3, getRequestedFor(urlPathMatching("/tg-chat/1000")));
    }
    @Test
    void noRetriesWhen200Response(){
        HttpExchangeAdapter wc = WebClientAdapter
            .create(webClient1
                .mutate()
                .filter(linear1())
                .build());

        HttpServiceProxyFactory httpServiceProxyFactory =
            HttpServiceProxyFactory
                .builderFor(wc)
                .build();
        var client = httpServiceProxyFactory.createClient(ScrapperClient.class);
        wm.stubFor(get(urlPathMatching("/tg-chat/1000"))
            .willReturn(aResponse()
                .withStatus(200)));
        try {
            client.findChat(1000);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        wm.verify(1, getRequestedFor(urlPathMatching("/tg-chat/1000")));
    }
}
