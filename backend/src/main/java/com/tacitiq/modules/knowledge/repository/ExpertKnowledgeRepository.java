package com.tacitiq.modules.knowledge.repository;

import com.tacitiq.modules.knowledge.entity.ExpertKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpertKnowledgeRepository extends JpaRepository<ExpertKnowledge, UUID> {
    List<ExpertKnowledge> findByEngineerId(UUID engineerId);
}
