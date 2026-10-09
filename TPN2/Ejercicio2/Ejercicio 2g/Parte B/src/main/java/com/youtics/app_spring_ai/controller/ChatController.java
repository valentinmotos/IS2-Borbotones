package com.youtics.app_spring_ai.controller;

import com.youtics.app_spring_ai.dto.ChatFullResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatClient client;

    @GetMapping
    public String chat(@RequestParam String message) {
        return client.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/full")
    public ChatFullResponse chatFullResponse(@RequestParam String message) {
        ChatResponse response = client.prompt()
                .user(message)
                .call()
                .chatResponse();
        Usage usage = response.getMetadata().getUsage();
        return new ChatFullResponse(
                response.getResult().getOutput().getText(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message) {
        return client.prompt()
                .user(message)
                .stream()
                .content();
    }
}
