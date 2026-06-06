package com.example.campuscrush.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.campuscrush.entity.feed.PublicConfession;
import com.example.campuscrush.entity.feed.PublicConfessionStatus;
import com.example.campuscrush.entity.user.User;

public interface PublicConfessionRepository extends JpaRepository<PublicConfession, Long> {

    // Unseen-first feed: LEFT JOIN on views so unseen posts sort to top.
    // Excludes expired posts and posts by blocked authors.
    @Query(value = """
        SELECT pc.* FROM public_confessions pc
        LEFT JOIN public_confession_views pv
               ON pv.confession_id = pc.id AND pv.viewer_id = :viewerId
        WHERE pc.campus_tag  = :campus
          AND pc.status      = :status
          AND pc.created_at  > :expiry
          AND pc.author_id NOT IN (
              SELECT blocked_id FROM user_blocks WHERE blocker_id = :viewerId
          )
        ORDER BY
          CASE WHEN pv.viewer_id IS NULL THEN 0 ELSE 1 END ASC,
          pc.id DESC
        LIMIT :lim
        """, nativeQuery = true)
    List<PublicConfession> findFeed(
        @Param("campus")   String campus,
        @Param("status")   String status,
        @Param("viewerId") Long viewerId,
        @Param("expiry")   Instant expiry,
        @Param("lim")      int lim
    );

    // Daily post count for rate limiting (3/day)
    long countByAuthorAndCreatedAtAfter(User author, Instant since);

    // Cleanup: delete posts older than the given threshold
    @Modifying
    @Transactional
    @Query("DELETE FROM PublicConfession pc WHERE pc.createdAt < :threshold")
    void deleteExpired(@Param("threshold") Instant threshold);
}
