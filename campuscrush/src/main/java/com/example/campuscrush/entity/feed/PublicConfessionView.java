package com.example.campuscrush.entity.feed;

import com.example.campuscrush.entity.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "public_confession_views")
@IdClass(PublicConfessionViewId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicConfessionView {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    private PublicConfession confession;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    private User viewer;
}
