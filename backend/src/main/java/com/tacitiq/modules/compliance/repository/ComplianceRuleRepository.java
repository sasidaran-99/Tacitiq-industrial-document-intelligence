package com.tacitiq.modules.compliance.repository;

import com.tacitiq.modules.compliance.entity.ComplianceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, UUID> {
}
