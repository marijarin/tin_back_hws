package edu.java.controller;

import edu.java.controller.dto.AddLinkRequest;
import edu.java.controller.dto.ChatResponse;
import edu.java.controller.dto.LinkResponse;
import edu.java.controller.dto.ListLinksResponse;
import edu.java.controller.dto.RemoveLinkRequest;
import edu.java.service.ChatService;
import edu.java.service.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("MultipleStringLiterals")
@RestController
public class ScrapperController {
    private final ChatService chatService;
    private final LinkService linkService;

    @Autowired
    public ScrapperController(ChatService chatService, LinkService linkService) {
        this.chatService = chatService;
        this.linkService = linkService;
    }

    @Operation(
        summary = "Зарегистрировать чат",
        responses = {
            @ApiResponse(responseCode = "200", description = "Чат зарегистрирован")
        })
    @PostMapping("/tg-chat/{id}")
    void registerChat(@PathVariable long id) {
        chatService.register(id);
    }

    @Operation(
        summary = "Найти чат",
        responses = {
            @ApiResponse(responseCode = "200", description = "Чат найден")
        })
    @GetMapping("/tg-chat/{id}")
    ChatResponse findChat(@PathVariable long id) {
        var chat = chatService.findChatById(id);
        return new ChatResponse(chat.getId());
    }

    @Operation(
        summary = "Удалить чат",
        responses = {
            @ApiResponse(responseCode = "200", description = "Чат успешно удалён")
        })
    @DeleteMapping("/tg-chat/{id}")
    void deleteChat(@PathVariable long id) {
        chatService.unregister(id);
    }

    @Operation(
        summary = "Получить все отслеживаемые ссылки",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ссылки успешно получены")
        })
    @GetMapping("/links")
    ListLinksResponse getLinks(@RequestHeader("Tg-Chat-Id") long tgChatId) {
        var links = linkService.listAll(tgChatId);
        if (links.isEmpty()) {
            throw new ResourceNotFoundException("Not found");
        }
        return new ListLinksResponse(
            links.stream().map(link -> new LinkResponse(link.getId(), link.getUri())).toList(),
            links.size()
        );
    }

    @Operation(
        summary = "Добавить отслеживание ссылки",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ссылка успешно добавлена")
        })
    @PostMapping("/links")
    LinkResponse startLinkTracking(
        @RequestHeader("Tg-Chat-Id") long tgChatId,
        @RequestBody @Valid AddLinkRequest linkRequest
    ) {
        linkService.add(tgChatId, linkRequest.link());
        return new LinkResponse(tgChatId, linkRequest.link());
    }

    @Operation(
        summary = "Убрать отслеживание ссылки",
        responses = {
            @ApiResponse(responseCode = "200", description = "Ссылка успешно убрана")
        })
    @DeleteMapping("/links")
    LinkResponse stopLinkTracking(
        @RequestHeader("Tg-Chat-Id") long tgChatId,
        @RequestBody @Valid RemoveLinkRequest linkRequest
    ) {
        linkService.remove(tgChatId, linkRequest.link());
        return new LinkResponse(tgChatId, linkRequest.link());
    }
}
