package com.example.campuscrush.entity.feed;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PublicConfessionViewId implements Serializable {
    private Long confession;
    private Long viewer;
}
