package com.example.campuscrush.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.confession.ConfessionState;
import com.example.campuscrush.entity.user.User;

public interface ConfessionRepository
        extends JpaRepository<Confession, Long> {

    boolean existsBySenderAndReceiverAndStateIn(
        User sender,
        User receiver,
        List<ConfessionState> states
    );

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Confession c WHERE c.sender = :user OR c.receiver = :user ORDER BY c.createdAt DESC")
    List<Confession> findAllByParticipant(User user);
}