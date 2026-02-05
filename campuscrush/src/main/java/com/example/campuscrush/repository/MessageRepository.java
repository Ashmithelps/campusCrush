package com.example.campuscrush.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.message.Message;

public interface MessageRepository
        extends JpaRepository<Message, Long> {

    List<Message> findByConfessionOrderBySentAtAsc(Confession confession);
}