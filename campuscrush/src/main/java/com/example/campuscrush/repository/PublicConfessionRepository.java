package com.example.campuscrush.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.campuscrush.entity.feed.PublicConfession;
import com.example.campuscrush.entity.feed.PublicConfessionStatus;
import com.example.campuscrush.entity.user.User;

public interface PublicConfessionRepository extends JpaRepository<PublicConfession, Long> {

    // Cursor-based feed: exclude posts by blocked authors, exclude already-viewed
    @Query("""
        SELECT pc FROM PublicConfession pc
        WHERE pc.campusTag = :campus
          AND pc.status = :status
          AND pc.id < :cursor
          AND pc.author NOT IN (
              SELECT ub.blocked FROM UserBlock ub WHERE ub.blocker = :viewer
          )
          AND pc.id NOT IN (
              SELECT pv.confession.id FROM PublicConfessionView pv WHERE pv.viewer = :viewer
          )
        ORDER BY pc.id DESC
        """)
    List<PublicConfession> findFeedWithCursor(
        @Param("campus") String campus,
        @Param("status") PublicConfessionStatus status,
        @Param("cursor") Long cursor,
        @Param("viewer") User viewer,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("""
        SELECT pc FROM PublicConfession pc
        WHERE pc.campusTag = :campus
          AND pc.status = :status
          AND pc.author NOT IN (
              SELECT ub.blocked FROM UserBlock ub WHERE ub.blocker = :viewer
          )
          AND pc.id NOT IN (
              SELECT pv.confession.id FROM PublicConfessionView pv WHERE pv.viewer = :viewer
          )
        ORDER BY pc.id DESC
        """)
    List<PublicConfession> findFeedNoCursor(
        @Param("campus") String campus,
        @Param("status") PublicConfessionStatus status,
        @Param("viewer") User viewer,
        org.springframework.data.domain.Pageable pageable
    );

    // Daily post count for rate limiting (3/day)
    long countByAuthorAndCreatedAtAfter(User author, Instant since);
}
