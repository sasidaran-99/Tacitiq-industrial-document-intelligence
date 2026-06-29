package com.tacitiq.modules.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatModel chatModel;
    private final boolean mockFallback;

    public ChatService(
            @Autowired(required = false) ChatModel chatModel,
            @Value("${tacitiq.ai.mock-fallback:true}") boolean mockFallback) {
        this.chatModel = chatModel;
        this.mockFallback = mockFallback;
    }

    public boolean isMockFallback() {
        return mockFallback;
    }

    public boolean isChatModelAvailable() {
        return chatModel != null;
    }

    public String generateResponse(String systemPrompt, String userMessage) {
        if (chatModel != null && !mockFallback) {
            try {
                log.info("Sending chat query to Gemini LLM...");
                SystemMessage systemMessage = new SystemMessage(systemPrompt);
                UserMessage userMsg = new UserMessage(userMessage);
                Prompt prompt = new Prompt(List.of(systemMessage, userMsg));
                return chatModel.call(prompt).getResult().getOutput().getContent();
            } catch (Exception e) {
                log.warn("Gemini LLM call failed. Falling back to offline mode.", e);
            }
        }
        return null;
    }
}
