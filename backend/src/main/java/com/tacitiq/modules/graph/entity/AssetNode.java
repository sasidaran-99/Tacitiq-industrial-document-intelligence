package com.tacitiq.modules.graph.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.*;
import java.util.List;

@Node("Asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetNode {

    @Id
    private String id; // UUID String representation

    @Property("tagNumber")
    private String tagNumber;

    @Property("assetType")
    private String assetType;

    @Relationship(type = "HAS_INCIDENT", direction = Relationship.Direction.OUTGOING)
    private List<IncidentNode> incidents;
}
