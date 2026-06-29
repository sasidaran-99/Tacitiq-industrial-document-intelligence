package com.tacitiq.modules.graph.service;

import com.tacitiq.modules.graph.entity.AssetNode;
import com.tacitiq.modules.graph.entity.FailureModeNode;
import com.tacitiq.modules.graph.entity.IncidentNode;
import com.tacitiq.modules.graph.entity.ProcedureNode;
import com.tacitiq.modules.graph.repository.AssetNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class GraphDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GraphDataSeeder.class);

    private final AssetNodeRepository assetNodeRepository;

    public GraphDataSeeder(AssetNodeRepository assetNodeRepository) {
        this.assetNodeRepository = assetNodeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            if (assetNodeRepository.count() == 0) {
                log.info("Seeding Neo4j database with asset nodes and RCA relationships...");

                // Create procedures (ID matches PR-01 / PR-02)
                ProcedureNode pr01 = ProcedureNode.builder()
                        .id("PR-01")
                        .title("SOP: Lubrication")
                        .storagePath("/docs/loto-sop.pdf")
                        .build();

                ProcedureNode pr02 = ProcedureNode.builder()
                        .id("PR-02")
                        .title("SOP: Lockout Tagout")
                        .storagePath("/docs/corrosion-sop.pdf")
                        .build();

                // Create failure modes (ID name matches FM-01 / FM-02)
                FailureModeNode fm01 = FailureModeNode.builder()
                        .name("FM-01")
                        .procedures(List.of(pr01))
                        .build();

                FailureModeNode fm02 = FailureModeNode.builder()
                        .name("FM-02")
                        .procedures(List.of(pr02))
                        .build();

                // Create incidents (ID matches INC-01 / INC-02)
                IncidentNode inc01 = IncidentNode.builder()
                        .id("INC-01")
                        .description("Why 1: Bearing Spike")
                        .severity("P2")
                        .failureMode(fm01)
                        .build();

                IncidentNode inc02 = IncidentNode.builder()
                        .id("INC-02")
                        .description("Why 1: LOTO Slip")
                        .severity("P1")
                        .failureMode(fm02)
                        .build();

                // Create assets
                AssetNode p101 = AssetNode.builder()
                        .id("c0000000-0000-0000-0000-000000000001")
                        .tagNumber("P-101")
                        .assetType("Centrifugal Pump")
                        .incidents(List.of(inc01))
                        .build();

                AssetNode k201 = AssetNode.builder()
                        .id("c0000000-0000-0000-0000-000000000002")
                        .tagNumber("K-201")
                        .assetType("Centrifugal Compressor")
                        .incidents(List.of(inc02))
                        .build();

                AssetNode e205 = AssetNode.builder()
                        .id("c0000000-0000-0000-0000-000000000003")
                        .tagNumber("E-205")
                        .assetType("Shell & Tube Heat Exchanger")
                        .build();

                assetNodeRepository.saveAll(List.of(p101, k201, e205));
                log.info("Neo4j database seeding completed successfully.");
            } else {
                log.info("Neo4j database already populated. Skipping seeding.");
            }
        } catch (Exception e) {
            log.error("Failed to seed Neo4j database. It might not be available yet or credentials incorrect.", e);
        }
    }
}
