package com.example.campuscrush.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.feed.PublicConfession;
import com.example.campuscrush.entity.feed.PublicConfessionReport;
import com.example.campuscrush.entity.user.User;

public interface PublicConfessionReportRepository extends JpaRepository<PublicConfessionReport, Long> {

    boolean existsByConfessionAndReporter(PublicConfession confession, User reporter);
}
