package com.example.campuscrush.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.dto.FeedItemResponse;
import com.example.campuscrush.entity.feed.PublicConfession;
import com.example.campuscrush.entity.feed.PublicConfessionReport;
import com.example.campuscrush.entity.feed.PublicConfessionStatus;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.PublicConfessionReportRepository;
import com.example.campuscrush.repository.PublicConfessionRepository;
import com.example.campuscrush.repository.PublicConfessionViewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final int  DAILY_POST_LIMIT   = 3;
    private static final int  MAX_CONTENT_LENGTH = 300;
    private static final int  AUTO_HIDE_REPORTS  = 3;
    private static final int  PAGE_SIZE          = 20;
    private static final long TTL_HOURS          = 24;

    private static final Set<String> BLOCKED_WORDS = Set.of(
        "fuck", "shit", "bitch", "asshole", "bastard", "cunt", "dick", "pussy", "whore", "slut"
    );

    private final PublicConfessionRepository confessionRepo;
    private final PublicConfessionViewRepository viewRepo;
    private final PublicConfessionReportRepository reportRepo;

    public List<FeedItemResponse> getFeed(User viewer) {
        String campus  = campusFromEmail(viewer.getCollegeEmail());
        Instant expiry = Instant.now().minus(TTL_HOURS, ChronoUnit.HOURS);
        List<PublicConfession> page = confessionRepo.findFeed(
            campus, PublicConfessionStatus.VISIBLE.name(), viewer.getId(), expiry,
            PageRequest.of(0, PAGE_SIZE));
        return page.stream().map(this::toResponse).toList();
    }

    @Transactional
    public FeedItemResponse post(User author, String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content cannot be empty");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max 300 characters");
        }
        if (containsBlockedWord(trimmed)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Content violates community guidelines");
        }

        Instant dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long todayCount = confessionRepo.countByAuthorAndCreatedAtAfter(author, dayStart);
        if (todayCount >= DAILY_POST_LIMIT) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily post limit reached");
        }

        PublicConfession pc = PublicConfession.builder()
            .author(author)
            .content(trimmed)
            .campusTag(campusFromEmail(author.getCollegeEmail()))
            .build();

        return toResponse(confessionRepo.save(pc));
    }

    @Transactional
    public void recordView(User viewer, Long confessionId) {
        PublicConfession pc = confessionRepo.findById(confessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        int inserted = viewRepo.insertIfAbsent(confessionId, viewer.getId());
        if (inserted == 1) {
            pc.setViewCount(pc.getViewCount() + 1);
            confessionRepo.save(pc);
        }
    }

    @Transactional
    public void report(User reporter, Long confessionId, String reason) {
        PublicConfession pc = confessionRepo.findById(confessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (reportRepo.existsByConfessionAndReporter(pc, reporter)) {
            return; // idempotent
        }

        PublicConfessionReport r = PublicConfessionReport.builder()
            .confession(pc)
            .reporter(reporter)
            .reason(reason != null ? reason.substring(0, Math.min(reason.length(), 100)) : null)
            .build();
        reportRepo.save(r);

        pc.setReportCount(pc.getReportCount() + 1);
        if (pc.getReportCount() >= AUTO_HIDE_REPORTS) {
            pc.setStatus(PublicConfessionStatus.HIDDEN);
        }
        confessionRepo.save(pc);
    }

    private FeedItemResponse toResponse(PublicConfession pc) {
        return new FeedItemResponse(pc.getId(), pc.getContent(), pc.getViewCount(), pc.getCreatedAt());
    }

    private String campusFromEmail(String email) {
        // "23bai70503@cuchd.in" → "cuchd"
        String domain = email.substring(email.indexOf('@') + 1);
        return domain.split("\\.")[0];
    }

    private boolean containsBlockedWord(String text) {
        String lower = text.toLowerCase();
        return BLOCKED_WORDS.stream().anyMatch(lower::contains);
    }
}
