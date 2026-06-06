package com.example.campuscrush.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.campuscrush.entity.feed.PublicConfession;
import com.example.campuscrush.entity.feed.PublicConfessionReport;
import com.example.campuscrush.entity.user.User;

public interface PublicConfessionReportRepository extends JpaRepository<PublicConfessionReport, Long> {

    boolean existsByConfessionAndReporter(PublicConfession confession, User reporter);

    @Modifying
    @Transactional
    @Query("DELETE FROM PublicConfessionReport pr WHERE pr.confession.createdAt < :threshold")
    void deleteByConfessionCreatedAtBefore(@Param("threshold") Instant threshold);
}
