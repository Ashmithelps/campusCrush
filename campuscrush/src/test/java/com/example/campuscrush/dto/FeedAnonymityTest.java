package com.example.campuscrush.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class FeedAnonymityTest {

    @Test
    void feedItemCarriesNoAuthorHandle() {
        Set<String> fields = Arrays.stream(FeedItemResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        // Fully anonymous: no author, no alias, no per-user identifier of any kind
        assertEquals(Set.of("id", "content", "viewCount", "createdAt"), fields);
    }
}
