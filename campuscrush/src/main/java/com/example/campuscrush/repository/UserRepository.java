package com.example.campuscrush.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.user.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCollegeEmail(String collegeEmail);

    Optional<User> findByPublicId(UUID publicId);

    Optional<User> findByRollNumber(String rollNumber);
}
