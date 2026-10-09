package com.youtics.app_spring_ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
    private static final String ASSISTANT = """
            Soy el asistente del canal de tecnología y programación y respondo en español.
            Solo hablo de temas relacionados con tecnología y programación.
            Si me preguntan de otra cosa, respondo amablemente que no puedo dar información sobre ese tema.
            """;

    @Bean
    public ChatClient client(ChatClient.Builder builder) {
        return builder.defaultSystem(ASSISTANT).build();
    }
}
