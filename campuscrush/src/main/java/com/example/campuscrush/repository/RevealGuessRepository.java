package com.example.campuscrush.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.reveal.RevealGuess;

public interface RevealGuessRepository extends JpaRepository<RevealGuess, Long> {
    List<RevealGuess> findByConfessionOrderByGuessedAtDesc(Confession confession);
}
