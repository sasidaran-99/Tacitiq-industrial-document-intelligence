package com.tacitiq.modules.ai.repository;

import com.tacitiq.modules.ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Query(value = "SELECT * FROM document_chunks ORDER BY embedding <=> CAST(:vectorString AS vector) LIMIT :limit", nativeQuery = true)
    List<DocumentChunk> findClosestChunks(@Param("vectorString") String vectorString, @Param("limit") int limit);

    @Query(value = "SELECT * FROM document_chunks WHERE document_id = :documentId ORDER BY chunk_index", nativeQuery = true)
    List<DocumentChunk> findByDocumentId(@Param("documentId") UUID documentId);
}
