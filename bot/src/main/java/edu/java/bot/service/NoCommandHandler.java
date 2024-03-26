package edu.java.bot.service;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import edu.java.bot.client.ScrapperClient;
import edu.java.bot.client.model.AddLinkRequest;
import edu.java.bot.client.model.LinkResponse;
import edu.java.bot.client.model.RemoveLinkRequest;
import edu.java.bot.configuration.ApplicationConfig;
import edu.java.bot.repository.CommandName;
import edu.java.bot.service.model.Bot;
import edu.java.bot.service.model.BotUser;
import java.net.URI;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoCommandHandler implements CommandHandler {
    private final ApplicationConfig applicationConfig;
    private final ScrapperClient scrapperClient;

    @Autowired public NoCommandHandler(ApplicationConfig applicationConfig, ScrapperClient scrapperClient) {
        this.scrapperClient = scrapperClient;
        this.applicationConfig = applicationConfig;
    }

    @Override
    public SendMessage handle(Bot bot, UserMessageHandler messageHandler, Update update) {
        BotUser botUser = messageHandler.extractUser(update);
        String text = messageHandler.convert(update).text();
        if (isBotHaving(bot, botUser) && bot.isWaiting().get(botUser) != null) {
            return convertToLink(botUser, text, bot);
        } else {
            return checkDB(bot, botUser, text);
        }
    }

    @Override
    public CommandName getCommand() {
        return CommandName.NOCOMMAND;
    }

    private SendMessage invalidUserMessage(long chatId) {
        return new SendMessage(
            chatId,
            applicationConfig.notUnderstand()
        );
    }

    private SendMessage convertToLink(BotUser botUser, String text, Bot bot) {
        var command = bot.isWaiting().get(botUser);
        bot.isWaiting().replace(botUser, null);
        Pattern pattern = Pattern.compile(applicationConfig.pattern());
        Matcher ulrMatcher = pattern.matcher(text.trim());
        if (ulrMatcher.find()) {
            return switch (command) {
                case TRACK -> processTrack(botUser, text, bot);
                case UNTRACK -> processUnTrack(botUser, text, bot);
                case null, default -> invalidUserMessage(botUser.chatId());
            };
        } else {
            return invalidUserMessage(botUser.chatId());
        }
    }

    private SendMessage processTrack(BotUser botUser, String url, Bot bot) {
        bot.isWaiting().replace(botUser, null);
        bot.chats().get(botUser).links().add(url);
        var linkRequest = new AddLinkRequest(URI.create(url));
        scrapperClient.startLinkTracking(botUser.chatId(), linkRequest);
        return new SendMessage(
            botUser.chatId(),
            applicationConfig.done() + url
        );
    }

    private SendMessage processUnTrack(BotUser botUser, String url, Bot bot) {
        bot.isWaiting().replace(botUser, null);
        var linkRequest = new RemoveLinkRequest(URI.create(url));
        LinkResponse linkResponse = scrapperClient.stopLinkTracking(botUser.chatId(), linkRequest);
        return handleLinkResponseInMemory(linkResponse, bot, botUser, url);
    }

    private SendMessage handleLinkResponseInMemory(LinkResponse linkResponse, Bot bot, BotUser botUser, String url) {
        var list = bot.chats().get(botUser).links();
        if (linkResponse != null && linkResponse.id() > 0 && linkResponse.url().toString().equals(url)) {
            bot.chats().get(botUser).links().remove(linkResponse.url().toString());
            return sendListLinks(bot, botUser);
        }
        if (list.remove(url)) {
            return sendListLinks(bot, botUser);
        }
        return new SendMessage(
            botUser.chatId(),
            applicationConfig.notTracked() + url
        );
    }

    private SendMessage sendListLinks(Bot bot, BotUser botUser) {
        return new SendMessage(
            botUser.chatId(),
            applicationConfig.done() + Arrays.deepToString(bot.chats().get(botUser).links().toArray())
        );
    }

    private SendMessage checkDB(Bot bot, BotUser botUser, String text) {
            var chatDB = scrapperClient.findChat(botUser.chatId());
            if (chatDB.chatId() == -1) {
                return askToRegister(botUser.chatId());
            }
            putUser(bot, botUser);
            return convertToLink(botUser, text, bot);
    }

    private SendMessage askToRegister(long chatId) {
        return new SendMessage(
            chatId,
            applicationConfig.register()
        );
    }
}
