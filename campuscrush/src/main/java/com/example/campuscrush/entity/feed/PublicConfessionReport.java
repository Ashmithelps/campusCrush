package com.example.campuscrush.entity.feed;

import java.time.Instant;

import com.example.campuscrush.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "public_confession_reports",
    uniqueConstraints = @UniqueConstraint(columnNames = {"confession_id", "reporter_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicConfessionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private PublicConfession confession;

    @ManyToOne(fetch = FetchType.LAZY)
    private User reporter;

    @Column(length = 100)
    private String reason;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
