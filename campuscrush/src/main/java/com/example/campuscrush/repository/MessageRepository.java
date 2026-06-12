package com.example.campuscrush.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.message.Message;
import com.example.campuscrush.entity.user.User;

public interface MessageRepository
        extends JpaRepository<Message, Long> {

    List<Message> findByConfessionOrderBySentAtAsc(Confession confession);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender = :sender AND m.sentAt >= :since")
    long countBySenderSince(@Param("sender") User sender, @Param("since") Instant since);
}