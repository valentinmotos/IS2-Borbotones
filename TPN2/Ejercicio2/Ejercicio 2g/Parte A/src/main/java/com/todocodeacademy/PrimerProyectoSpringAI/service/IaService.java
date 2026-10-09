package com.todocodeacademy.PrimerProyectoSpringAI.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class IaService {
    private final ChatClient chatClient;

    public IaService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String preguntar(String pregunta) {
        return chatClient.prompt()
                .system("Sos un profesor especializado en programación. Respondé siempre en español y explicá los conceptos de forma sencilla.")
                .user(pregunta)
                .call()
                .content();
    }
}
