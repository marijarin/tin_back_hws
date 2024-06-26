package edu.java.bot.configuration;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public record ApplicationConfig(
    @NotEmpty
    String telegramToken,
    @NotEmpty
    String register,
    @NotEmpty
    String registered,
    @NotEmpty
    String alreadyRegistered,
    @NotEmpty
    String notUnderstand,
    @NotEmpty
    String sendLink,
    @NotEmpty
    String linksHeader,
    @NotEmpty
    String emptyList,
    @NotEmpty
    String done,
    @NotEmpty
    String pattern,
    @NotEmpty
    String notTracked,
    @NotEmpty
    String baseUrlScrapper,

    @NotEmpty
    String seeUpdate,
    @NotEmpty
    String deleteWithSecretPhrase,
    @NotEmpty
    String typeLinear,
    @NotEmpty
    String typeConstant,

    @NotEmpty

    String typeExponential,

    List<String> errorFilters,

    int filterCode
) {

}
