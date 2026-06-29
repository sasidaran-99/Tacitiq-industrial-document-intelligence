package com.tacitiq.modules.graph.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.*;

@Node("Incident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentNode {

    @Id
    private String id; // UUID String representation

    @Property("description")
    private String description;

    @Property("severity")
    private String severity;

    @Relationship(type = "CAUSED_BY", direction = Relationship.Direction.OUTGOING)
    private FailureModeNode failureMode;
}
