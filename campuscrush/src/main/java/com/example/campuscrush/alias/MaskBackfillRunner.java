package com.example.campuscrush.alias;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.repository.ConfessionRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-time backfill: assigns per-conversation masks to confessions created
 * under the old per-user alias scheme. Inbox-scoped unique per viewer.
 * Self-healing — exits immediately once no unmasked confessions remain.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaskBackfillRunner {

    private final ConfessionRepository confessionRepository;
    private final AliasGenerator aliasGenerator;

    @PostConstruct
    public void backfill() {
        List<Confession> missing = confessionRepository.findBySenderAliasIsNull();
        if (missing.isEmpty()) {
            return;
        }

        // Track masks assigned in this run per viewer (receiver, or invited
        // roll number) so the batch stays inbox-unique before any flush
        Map<String, Set<String>> assignedByViewer = new HashMap<>();

        for (Confession c : missing) {
            String viewerKey = c.getReceiver() != null
                    ? "u:" + c.getReceiver().getId()
                    : "r:" + c.getReceiverRollNumber();
            Set<String> assigned = assignedByViewer.computeIfAbsent(viewerKey, k -> new HashSet<>());

            String mask = aliasGenerator.generate(candidate ->
                assigned.contains(candidate)
                || (c.getReceiver() != null
                        ? confessionRepository.existsByReceiverAndSenderAlias(c.getReceiver(), candidate)
                        : confessionRepository.existsByReceiverRollNumberAndSenderAlias(c.getReceiverRollNumber(), candidate)));

            c.setSenderAlias(mask);
            assigned.add(mask);
            confessionRepository.save(c);
        }

        log.info("MaskBackfill: assigned per-conversation masks to {} confession(s)", missing.size());
    }
}
