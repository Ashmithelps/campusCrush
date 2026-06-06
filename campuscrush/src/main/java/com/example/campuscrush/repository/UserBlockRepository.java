package com.example.campuscrush.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.entity.user.UserBlock;
import com.example.campuscrush.entity.user.UserBlockId;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    boolean existsByBlockerAndBlocked(User blocker, User blocked);
}
