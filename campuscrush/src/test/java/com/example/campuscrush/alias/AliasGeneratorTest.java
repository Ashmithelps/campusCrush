package com.example.campuscrush.alias;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AliasGeneratorTest {

    private final AliasGenerator generator = new AliasGenerator();

    @Test
    void returnsTwoWordMaskWhenScopeIsEmpty() {
        String mask = generator.generate(candidate -> false);
        assertTrue(mask.matches("^\\S+ \\S+$"), "expected 'Adjective Noun', got: " + mask);
    }

    @Test
    void retriesUntilMaskIsFreeWithinScope() {
        // Reject every bare two-word mask — forces the suffix loop
        String mask = generator.generate(candidate -> !candidate.endsWith(" 2"));
        assertTrue(mask.endsWith(" 2"), "expected suffix retry, got: " + mask);
    }

    @Test
    void neverReturnsataTakenMask() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String mask = generator.generate(seen::contains);
            assertFalse(seen.contains(mask));
            seen.add(mask);
        }
    }
}
