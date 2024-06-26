package edu.java.service;

import edu.java.client.model.LinkUpdate;
import edu.java.domain.model.ChatDao;
import edu.java.service.model.EventLink;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScrapperQueueProducer {
    private final LinkUpdater linkUpdater;
    private final ChatService chatService;

    private final Logger logger = LogManager.getLogger();

    private final KafkaTemplate<String, LinkUpdate> kafkaTemplateScrapper;

    @Autowired
    public ScrapperQueueProducer(
        LinkUpdater linkUpdater,
        ChatService chatService,
        KafkaTemplate<String, LinkUpdate> kafkaTemplate
    ) {
        this.linkUpdater = linkUpdater;
        this.chatService = chatService;
        this.kafkaTemplateScrapper = kafkaTemplate;
    }

    public List<LinkUpdate> send() throws ExecutionException, InterruptedException {

        // bad message example for fast check >>

//        kafkaTemplateScrapper
//            .send("messages.string",
//            //  1,
//            //OffsetDateTime.now().toString(),
//                new LinkUpdate(1L, URI.create("https://stackoverflow.com"), "12345", List.of(618490579L), "hello exception"));

        var links = linkUpdater.update();
        if (!links.isEmpty()) {
            var linkUpdates = links.stream().map(this::makeItFromLink).toList();
            var results = new ArrayList<LinkUpdate>();
            for (LinkUpdate li : linkUpdates) {
                var result = kafkaTemplateScrapper
                    .send("messages.string", 1, li.id() + li.description(), li)
                    .get();
                results.add(result.getProducerRecord().value());
                logger.info(result);
            }
            return results;
        }
        return List.of();
    }

    private LinkUpdate makeItFromLink(EventLink eventLink) {
        return new LinkUpdate(
            eventLink.getLink().getId(),
            eventLink.getLink().getUri(),
            eventLink.getEvent().getDescription(),
            chatService.findAllChatsWithLink(eventLink.getLink().getUri()).stream().map(ChatDao::getId).toList(),
            eventLink.getEvent().getDescription()
        );
    }
}
