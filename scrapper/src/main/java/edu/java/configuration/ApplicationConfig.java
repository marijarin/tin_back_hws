package edu.java.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ApplicationConfig(
    @NotNull
    @Bean
    Scheduler scheduler,
    @NotEmpty
    String baseUrlGitHub,
    @NotEmpty
    String baseUrlStackOverflow,
    @NotEmpty
    String baseUrlBot,
    AccessType databaseAccessType,
    @NotEmpty
    String typeLinear,
    @NotEmpty
    String typeConstant,

    @NotEmpty

    String typeExponential,

    List<String> errorFilters,

    int filterCode,
    RateLimit read,
    RateLimit write
) {
    @Bean
    public DefaultConfigurationCustomizer postgresJooqCustomizer() {
        return (DefaultConfiguration c) -> c.settings()
            .withRenderSchema(false)
            .withRenderFormatted(true)
            .withRenderQuotedNames(RenderQuotedNames.NEVER);
    }

    public record Scheduler(boolean enable, @NotNull Duration interval, @NotNull Duration forceCheckDelay) {
    }

    public enum AccessType {
        JDBC,
        JPA,
        JOOQ
    }

    public record RateLimit(int count, int tokens, int period) {
    }

    public record ReadWriteLimit(RateLimit read, RateLimit write) {
    }
}
