package com.tacitiq.modules.graph.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import lombok.*;

@Node("Procedure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcedureNode {

    @Id
    private String id; // UUID String representation

    @Property("title")
    private String title;

    @Property("storagePath")
    private String storagePath;
}
