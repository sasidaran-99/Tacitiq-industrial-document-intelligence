package com.tacitiq.modules.graph.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.*;
import java.util.List;

@Node("FailureMode")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureModeNode {

    @Id
    private String name; // e.g. "Bearing Failure", "Mechanical Seal Leak"

    @Relationship(type = "MITIGATED_BY", direction = Relationship.Direction.OUTGOING)
    private List<ProcedureNode> procedures;
}
