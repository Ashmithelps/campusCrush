package com.example.campuscrush.alias;

import java.util.Random;

import org.springframework.stereotype.Component;

@Component
public class AliasGenerator {

    private static final String[] COLORS = {
        "Blue", "Red", "Purple", "Green", "Golden"
    };

    private static final String[] ANIMALS = {
        "Panda", "Fox", "Otter", "Tiger", "Falcon"
    };

    private final Random random = new Random();

    public String generate() {
        return COLORS[random.nextInt(COLORS.length)] + " " +
               ANIMALS[random.nextInt(ANIMALS.length)];
    }
}
