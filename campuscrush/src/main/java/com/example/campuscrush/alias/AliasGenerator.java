package com.example.campuscrush.alias;

import java.util.Random;

import org.springframework.stereotype.Component;

import com.example.campuscrush.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AliasGenerator {

    private final UserRepository userRepository;

    private static final String[] COLORS = {
        "Blue", "Red", "Purple", "Green", "Golden", "Silver", "Crimson",
        "Indigo", "Amber", "Teal", "Coral", "Jade", "Scarlet", "Violet", "Ivory"
    };

    private static final String[] ANIMALS = {
        "Panda", "Fox", "Otter", "Tiger", "Falcon", "Wolf", "Lynx",
        "Raven", "Bear", "Hawk", "Deer", "Crow", "Viper", "Crane", "Bison",
        "Mole", "Newt", "Wren", "Ibis", "Finch"
    };

    private final Random random = new Random();

    public String generate() {
        String base = COLORS[random.nextInt(COLORS.length)] + " " +
                      ANIMALS[random.nextInt(ANIMALS.length)];

        if (!userRepository.existsByDisplayAlias(base)) {
            return base;
        }

        // Collision — append a number until unique
        for (int i = 2; i <= 999; i++) {
            String candidate = base + " " + i;
            if (!userRepository.existsByDisplayAlias(candidate)) {
                return candidate;
            }
        }

        // Fallback (effectively impossible at 1000 users)
        return base + " " + System.currentTimeMillis();
    }
}
