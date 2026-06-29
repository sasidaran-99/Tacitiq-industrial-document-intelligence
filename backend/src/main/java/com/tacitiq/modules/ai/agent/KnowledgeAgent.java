package com.tacitiq.modules.ai.agent;

import com.tacitiq.modules.ai.service.ChatService;
import com.tacitiq.modules.ai.service.ContextBuilder;
import com.tacitiq.modules.ai.service.PromptBuilder;
import com.tacitiq.modules.ai.service.QueryRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KnowledgeAgent {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAgent.class);

    private final QueryRouter queryRouter;
    private final ContextBuilder contextBuilder;
    private final PromptBuilder promptBuilder;
    private final ChatService chatService;

    public KnowledgeAgent(
            QueryRouter queryRouter,
            ContextBuilder contextBuilder,
            PromptBuilder promptBuilder,
            ChatService chatService) {
        this.queryRouter = queryRouter;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.chatService = chatService;
    }

    public String answerQuery(String query, String userContext, Map<String, Object> chatContext) {
        log.info("Knowledge Agent executing orchestrator pipeline for query: '{}'", query);

        // 1. Determine intent using QueryRouter
        QueryRouter.RoutingResult routing = queryRouter.route(query, chatContext);

        // 2. Fetch live data context using ContextBuilder
        Map<String, Object> data = contextBuilder.buildContext(
                routing.getIntent(), 
                routing.getTargetAssetTag(), 
                routing.isMissingAssetContext(), 
                chatContext, 
                query
        );

        // 3. Resolve using live LLM or offline template renderer
        if (!chatService.isMockFallback() && chatService.isChatModelAvailable()) {
            String systemPrompt = promptBuilder.buildSystemPrompt(data);
            String response = chatService.generateResponse(systemPrompt, query);
            if (response != null) {
                return response;
            }
        }

        // Offline template fallback
        return promptBuilder.renderOfflineResponse(data);
    }

    public String answerQuery(String query, String userContext) {
        return answerQuery(query, userContext, new ConcurrentHashMap<>());
    }
}
