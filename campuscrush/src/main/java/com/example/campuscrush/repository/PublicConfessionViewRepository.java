package com.example.campuscrush.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.campuscrush.entity.feed.PublicConfessionView;
import com.example.campuscrush.entity.feed.PublicConfessionViewId;

public interface PublicConfessionViewRepository extends JpaRepository<PublicConfessionView, PublicConfessionViewId> {

    // Returns 1 if newly inserted, 0 if already existed — used for atomic view count
    @Modifying
    @Query(value = """
        INSERT INTO public_confession_views (confession_id, viewer_id)
        VALUES (:confessionId, :viewerId)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("confessionId") Long confessionId, @Param("viewerId") Long viewerId);
}
