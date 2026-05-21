package com.hust.aiservice.repository;

import com.hust.aiservice.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO document_chunks (id, course_id, lesson_id, content, content_type, source_citation, embedding) " +
                   "VALUES (:id, :courseId, :lessonId, :content, :contentType, :sourceCitation, cast(:embedding as vector))", 
           nativeQuery = true)
    void insertChunk(
        @Param("id") String id,
        @Param("courseId") String courseId,
        @Param("lessonId") String lessonId,
        @Param("content") String content,
        @Param("contentType") String contentType,
        @Param("sourceCitation") String sourceCitation,
        @Param("embedding") String embeddingString
    );

    @Query(value = "SELECT id, course_id as courseId, lesson_id as lessonId, content, content_type as contentType, source_citation as sourceCitation, " +
                   "(embedding <=> cast(:queryVector as vector)) as score " +
                   "FROM document_chunks " +
                   "WHERE course_id = :courseId " +
                   "ORDER BY score ASC LIMIT :limit",
           nativeQuery = true)
    java.util.List<Object[]> vectorSearch(
        @Param("courseId") String courseId,
        @Param("queryVector") String queryVectorString,
        @Param("limit") int limit
    );

    @Query(value = "SELECT id, course_id as courseId, lesson_id as lessonId, content, content_type as contentType, source_citation as sourceCitation, " +
                   "ts_rank_cd(fts_document, websearch_to_tsquery('simple', :queryText)) as score " +
                   "FROM document_chunks " +
                   "WHERE course_id = :courseId " +
                   "AND fts_document @@ websearch_to_tsquery('simple', :queryText) " +
                   "ORDER BY score DESC LIMIT :limit",
           nativeQuery = true)
    java.util.List<Object[]> ftsSearch(
        @Param("courseId") String courseId,
        @Param("queryText") String queryText,
        @Param("limit") int limit
    );
}
