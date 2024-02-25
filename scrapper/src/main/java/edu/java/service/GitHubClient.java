package edu.java.service;

import edu.java.model.ResponseGitHubDTO;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange(url = "/repos/{owner}/{repo}", accept = "application/vnd.github.v3+json")
public interface GitHubClient {
    @GetExchange("/events")
    Mono<List<ResponseGitHubDTO>> getResponse(@PathVariable String owner, @PathVariable String repo);
}
