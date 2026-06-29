package com.tacitiq.modules.ai.service;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationContextStore {

    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    public Map<String, Object> getContext(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return new ConcurrentHashMap<>();
        }
        return store.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>());
    }

    public void saveContext(String conversationId, Map<String, Object> context) {
        if (conversationId != null && !conversationId.isBlank() && context != null) {
            store.put(conversationId, context);
        }
    }

    public void clearContext(String conversationId) {
        if (conversationId != null) {
            store.remove(conversationId);
        }
    }
}
