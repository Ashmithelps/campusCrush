package com.example.campuscrush.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.reveal.RevealState;

public interface RevealStateRepository extends JpaRepository<RevealState, Long> {
    Optional<RevealState> findByConfession(Confession confession);
}
