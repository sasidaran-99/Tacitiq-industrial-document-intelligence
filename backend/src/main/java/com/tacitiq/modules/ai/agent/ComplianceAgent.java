package com.tacitiq.modules.ai.agent;

import com.tacitiq.modules.ai.service.ChatService;
import com.tacitiq.modules.compliance.entity.ComplianceRule;
import com.tacitiq.modules.compliance.repository.ComplianceRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ComplianceAgent {

    private static final Logger log = LoggerFactory.getLogger(ComplianceAgent.class);

    private final ChatService chatService;
    private final ComplianceRuleRepository ruleRepository;

    public ComplianceAgent(ChatService chatService, ComplianceRuleRepository ruleRepository) {
        this.chatService = chatService;
        this.ruleRepository = ruleRepository;
    }

    public String auditProcedureCompliance(String procedureTitle, String procedureText) {
        log.info("Compliance Agent auditing procedure: '{}'", procedureTitle);

        List<ComplianceRule> rules = ruleRepository.findAll();
        StringBuilder rulesContext = new StringBuilder("Applicable safety regulations standards:\n");
        for (ComplianceRule rule : rules) {
            rulesContext.append(String.format("- Standard: %s, Clause: %s, Description: %s\n",
                    rule.getStandard(), rule.getClause(), rule.getDescription()));
        }

        String systemPrompt = String.format("""
            You are TacitIQ's Compliance and Governance Agent.
            Analyze the provided procedure text against safety regulations standards to detect gaps.
            
            Procedure Title: %s
            Procedure Content:
            %s
            
            %s
            
            Output a JSON or markdown report with:
            1. Audited Rule Matches
            2. Compliance Status (COMPLIANT, PARTIALLY_COMPLIANT, NON_COMPLIANT)
            3. Detailed Gaps found (e.g. missing LOTO warning isolation steps)
            4. Corrective Action requirements
            """, procedureTitle, procedureText, rulesContext);

        return chatService.generateResponse(systemPrompt, "Audit compliance for procedure: " + procedureTitle);
    }
}
