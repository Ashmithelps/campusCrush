package com.example.campuscrush.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    java.util.Optional<Confession> findFirstBySenderAndReceiverAndStateIn(
        User sender,
        User receiver,
        List<ConfessionState> states
    );

    @Query("SELECT c FROM Confession c JOIN FETCH c.sender LEFT JOIN FETCH c.receiver WHERE c.sender = :user OR c.receiver = :user ORDER BY c.createdAt DESC")
    List<Confession> findAllByParticipant(@Param("user") User user);

    @Query("SELECT COUNT(c) FROM Confession c WHERE c.sender = :sender AND c.createdAt >= :since")
    long countBySenderSince(@Param("sender") User sender, @Param("since") Instant since);

    List<Confession> findByReceiverRollNumberAndState(String receiverRollNumber, ConfessionState state);

    boolean existsBySenderAndReceiverRollNumberAndState(User sender, String receiverRollNumber, ConfessionState state);

    @Query("SELECT MAX(c.lastInviteSentAt) FROM Confession c WHERE c.receiverRollNumber = :rollNumber AND c.lastInviteSentAt IS NOT NULL")
    java.util.Optional<java.time.Instant> findLatestInviteSentAtForRollNumber(@Param("rollNumber") String rollNumber);

    // Mask uniqueness is scoped to one viewer's inbox — never campus-wide
    boolean existsByReceiverAndSenderAlias(User receiver, String senderAlias);

    boolean existsByReceiverRollNumberAndSenderAlias(String receiverRollNumber, String senderAlias);

    List<Confession> findBySenderAliasIsNull();

    @Query("SELECT COUNT(c) > 0 FROM Confession c WHERE c.state = com.example.campuscrush.entity.confession.ConfessionState.BLOCKED " +
           "AND ((c.sender = :a AND c.receiver = :b) OR (c.sender = :b AND c.receiver = :a))")
    boolean existsBlockedBetween(@Param("a") User a, @Param("b") User b);

    @Query("SELECT COUNT(DISTINCT c.receiver.id) FROM Confession c WHERE c.sender = :sender AND c.createdAt >= :since AND c.receiver IS NOT NULL")
    long countDistinctReceiversSince(@Param("sender") User sender, @Param("since") Instant since);

    @Query("SELECT COUNT(DISTINCT c.receiverRollNumber) FROM Confession c WHERE c.sender = :sender AND c.createdAt >= :since AND c.receiverRollNumber IS NOT NULL")
    long countDistinctInvitedRollsSince(@Param("sender") User sender, @Param("since") Instant since);

    long countBySenderAndStateIn(User sender, List<ConfessionState> states);
}