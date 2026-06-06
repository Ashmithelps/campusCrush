package com.example.campuscrush.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.reveal.RevealKit;
import com.example.campuscrush.entity.user.User;

public interface RevealKitRepository extends JpaRepository<RevealKit, Long> {
    Optional<RevealKit> findByUser(User user);
}
