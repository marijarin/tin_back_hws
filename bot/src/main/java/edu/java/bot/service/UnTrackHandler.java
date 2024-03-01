package edu.java.bot.service;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import edu.java.bot.configuration.ApplicationConfig;
import edu.java.bot.repository.CommandName;
import edu.java.bot.service.model.Bot;
import edu.java.bot.service.model.BotUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UnTrackHandler implements CommandHandler {
    private final ApplicationConfig applicationConfig;

    @Autowired
    private UnTrackHandler(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public UnTrackHandler(ApplicationConfig applicationConfig, boolean isTest) {
        this.applicationConfig = applicationConfig;
    }

    @Override
    public SendMessage handle(Bot bot, UserMessageHandler messageHandler, Update update) {
        BotUser botUser = messageHandler.extractUser(update);
        if (isBotHaving(bot, botUser)) {
            return waitForALink(botUser, bot);
        } else {
            return askToRegister(botUser.chatId());
        }
    }

    public SendMessage askToRegister(long chatId) {
        return new SendMessage(
            chatId,
            applicationConfig.register()
        );
    }

    public SendMessage waitForALink(BotUser botUser, Bot bot) {
        bot.isWaiting().replace(botUser, getCommand());
        return new SendMessage(
            botUser.chatId(),
            applicationConfig.sendLink()
        );
    }

    @Override
    public CommandName getCommand() {
        return CommandName.UNTRACK;
    }

}
