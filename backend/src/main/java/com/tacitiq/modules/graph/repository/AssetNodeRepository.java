package com.tacitiq.modules.graph.repository;

import com.tacitiq.modules.graph.entity.AssetNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AssetNodeRepository extends Neo4jRepository<AssetNode, String> {

    Optional<AssetNode> findByTagNumber(String tagNumber);

    // Custom Cypher query to fetch visual graph nodes and relationships for the Cytoscape canvas
    @Query("MATCH (a:Asset)-[r:HAS_INCIDENT]->(i:Incident)-[c:CAUSED_BY]->(f:FailureMode)-[m:MITIGATED_BY]->(p:Procedure) " +
           "RETURN a, r, i, c, f, m, p")
    List<Map<String, Object>> getFullCytoscapeSchema();

    // Query for 5-Why root cause traversal path starting from an asset
    @Query("MATCH path = (a:Asset {tagNumber: $tagNumber})-[r:HAS_INCIDENT]->(i:Incident)-[:CAUSED_BY]->(f:FailureMode)-[:MITIGATED_BY]->(p:Procedure) " +
           "RETURN path")
    List<Map<String, Object>> getAssetRcaPath(@Param("tagNumber") String tagNumber);
}
