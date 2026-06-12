package com.example.campuscrush.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.reveal.RevealKit;

public interface RevealKitRepository extends JpaRepository<RevealKit, Long> {
    Optional<RevealKit> findByConfession(Confession confession);
}
