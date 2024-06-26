package edu.java.hw1;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;
import edu.java.bot.configuration.ApplicationConfig;
import edu.java.bot.repository.CommandName;
import edu.java.bot.service.CommandHandler;
import edu.java.bot.service.PenBot;
import edu.java.bot.service.UserMessageHandler;
import edu.java.bot.service.UserMessageHandlerImpl;
import edu.java.bot.service.model.Bot;
import edu.java.bot.service.model.BotUser;
import edu.java.bot.service.model.Chat;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.step.StepCounter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class PenBotTest {
    @Mock TelegramBot telegramBot = new TelegramBot("12345");
    BotUser botUser = new BotUser(1, 123, "Test User", false);
    HashMap<BotUser, CommandName> isWaiting = new HashMap<>();

    @Mock Update update = new Update();

    @Mock Message message = new Message();

    @Mock ApplicationConfig applicationConfig;

    @Mock MeterRegistry meterRegistry;

    User user = new User(1L);
    com.pengrad.telegrambot.model.Chat chat = mock(com.pengrad.telegrambot.model.Chat.class);

    @ParameterizedTest
    @EnumSource(CommandName.class)
    void processesUpdatesWithNoExceptions(CommandName command) {
        Chat chat1 = new Chat(
            botUser.chatId(),
            botUser.id(),
            botUser.name(),
            new HashSet<>()
        );
        isWaiting.put(botUser, null);
        Bot bot = new Bot(
            telegramBot,
            Map.of(botUser, chat1),
            isWaiting
        );
        Map<String, CommandHandler> commandHandlers = Map.of(command.name(), new CommandHandler() {
            @Override
            public SendMessage handle(Bot bot, UserMessageHandler userMessageHandler, Update update) {
                return new SendMessage(botUser.chatId(), command.name());
            }

            @Override
            public CommandName getCommand() {
                return command;
            }
        });
        try (PenBot penBot = new PenBot(bot,
            applicationConfig,
            new UserMessageHandlerImpl(),
            commandHandlers,
            meterRegistry
        )) {
            penBot.start();
            Mockito.when(update.message()).thenReturn(message);
            Mockito.when(message.text()).thenReturn(command.getCommand());
            Mockito.when(message.chat()).thenReturn(chat);
            Mockito.when(message.chat().id()).thenReturn(1L);
            Mockito.when(message.from()).thenReturn(user);
            penBot.processedUserMessagesCounter = new StepCounter (new Meter.Id("", Tags.empty(), "", "", Meter.Type.COUNTER), Clock.SYSTEM, 1000);

            int result = penBot.process(List.of(update));

            assertThat(result).isEqualTo(-1);
        }
    }
}
