package com.jobconnect.news;

import com.jobconnect.news.dto.NewsAdminListResponse;
import com.jobconnect.news.dto.NewsListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NewsRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<NewsArticle> findBySlug(String slug);

    // ================= PUBLIC LIST - KHÔNG SELECT CONTENT =================

    @Query(
            value = """
                    SELECT new com.jobconnect.news.dto.NewsListResponse(
                        n.id,
                        n.title,
                        n.slug,
                        n.summary,
                        n.thumbnailUrl,
                        n.publishedAt,
                        n.viewCount
                    )
                    FROM NewsArticle n
                    WHERE n.status = 'PUBLISHED'
                    AND (n.publishedAt IS NULL OR n.publishedAt <= :now)
                    ORDER BY n.publishedAt DESC, n.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(n)
                    FROM NewsArticle n
                    WHERE n.status = 'PUBLISHED'
                    AND (n.publishedAt IS NULL OR n.publishedAt <= :now)
                    """
    )
    Page<NewsListResponse> findPublicPublishedList(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT new com.jobconnect.news.dto.NewsListResponse(
                        n.id,
                        n.title,
                        n.slug,
                        n.summary,
                        n.thumbnailUrl,
                        n.publishedAt,
                        n.viewCount
                    )
                    FROM NewsArticle n
                    WHERE n.status = 'PUBLISHED'
                    AND (n.publishedAt IS NULL OR n.publishedAt <= :now)
                    AND LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    ORDER BY n.publishedAt DESC, n.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(n)
                    FROM NewsArticle n
                    WHERE n.status = 'PUBLISHED'
                    AND (n.publishedAt IS NULL OR n.publishedAt <= :now)
                    AND LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    """
    )
    Page<NewsListResponse> searchPublicPublishedList(
            @Param("keyword") String keyword,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM NewsArticle n
            WHERE n.slug = :slug
            AND n.status = 'PUBLISHED'
            AND (n.publishedAt IS NULL OR n.publishedAt <= :now)
            """)
    Optional<NewsArticle> findPublicDetail(
            @Param("slug") String slug,
            @Param("now") LocalDateTime now
    );

    // ================= ADMIN LIST - KHÔNG SELECT CONTENT =================

    @Query(
            value = """
                    SELECT new com.jobconnect.news.dto.NewsAdminListResponse(
                        n.id,
                        n.title,
                        n.slug,
                        n.summary,
                        n.thumbnailUrl,
                        n.status,
                        n.publishedAt,
                        n.viewCount,
                        n.createdBy,
                        n.createdAt,
                        n.updatedAt
                    )
                    FROM NewsArticle n
                    ORDER BY n.createdAt DESC
                    """,
            countQuery = "SELECT COUNT(n) FROM NewsArticle n"
    )
    Page<NewsAdminListResponse> findAdminList(Pageable pageable);

    @Query(
            value = """
                    SELECT new com.jobconnect.news.dto.NewsAdminListResponse(
                        n.id,
                        n.title,
                        n.slug,
                        n.summary,
                        n.thumbnailUrl,
                        n.status,
                        n.publishedAt,
                        n.viewCount,
                        n.createdBy,
                        n.createdAt,
                        n.updatedAt
                    )
                    FROM NewsArticle n
                    WHERE n.status = :status
                    ORDER BY n.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(n)
                    FROM NewsArticle n
                    WHERE n.status = :status
                    """
    )
    Page<NewsAdminListResponse> findAdminListByStatus(
            @Param("status") NewsStatus status,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT new com.jobconnect.news.dto.NewsAdminListResponse(
                        n.id,
                        n.title,
                        n.slug,
                        n.summary,
                        n.thumbnailUrl,
                        n.status,
                        n.publishedAt,
                        n.viewCount,
                        n.createdBy,
                        n.createdAt,
                        n.updatedAt
                    )
                    FROM NewsArticle n
                    WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    ORDER BY n.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(n)
                    FROM NewsArticle n
                    WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    """
    )
    Page<NewsAdminListResponse> searchAdminList(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT new com.jobconnect.news.dto.NewsAdminListResponse(
                        n.id,
                        n.title,
                        n.slug,
                        n.summary,
                        n.thumbnailUrl,
                        n.status,
                        n.publishedAt,
                        n.viewCount,
                        n.createdBy,
                        n.createdAt,
                        n.updatedAt
                    )
                    FROM NewsArticle n
                    WHERE n.status = :status
                    AND LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    ORDER BY n.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(n)
                    FROM NewsArticle n
                    WHERE n.status = :status
                    AND LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    """
    )
    Page<NewsAdminListResponse> searchAdminListByStatus(
            @Param("keyword") String keyword,
            @Param("status") NewsStatus status,
            Pageable pageable
    );

    // ================= VIEW COUNT =================

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NewsArticle n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    void increaseViewCount(@Param("id") Long id);
}