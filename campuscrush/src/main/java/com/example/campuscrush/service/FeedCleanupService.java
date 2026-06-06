package com.example.campuscrush.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campuscrush.repository.PublicConfessionReportRepository;
import com.example.campuscrush.repository.PublicConfessionRepository;
import com.example.campuscrush.repository.PublicConfessionViewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedCleanupService {

    private final PublicConfessionRepository    confessionRepo;
    private final PublicConfessionViewRepository viewRepo;
    private final PublicConfessionReportRepository reportRepo;

    // Hourly: purge confessions older than 24h (children first — no CASCADE on FKs).
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void deleteExpiredConfessions() {
        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        viewRepo.deleteByConfessionCreatedAtBefore(threshold);
        reportRepo.deleteByConfessionCreatedAtBefore(threshold);
        confessionRepo.deleteExpired(threshold);
    }
}
