package com.example.campuscrush.alias;

import java.util.Random;

import org.springframework.stereotype.Component;

import com.example.campuscrush.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AliasGenerator {

    private final UserRepository userRepository;

    private static final String[] ADJECTIVES = {
        "Velvet", "Amber", "Hollow", "Silver", "Pale", "Dusk", "Soft", "Quiet",
        "Faded", "Gentle", "Lucid", "Sable", "Wistful", "Muted", "Ivory", "Veiled",
        "Tender", "Fleeting", "Ashen", "Gilded", "Ember", "Twilight", "Distant",
        "Serene", "Languid", "Petal", "Briar", "Misty", "Sparse", "Lunar", "Fond",
        "Still", "Tidal", "Sunken", "Fervent", "Adrift", "Solemn", "Lush", "Smoky",
        "Woven", "Bleak", "Mossy", "Brazen", "Pensive", "Verdant", "Stark", "Lilac",
        "Mellow", "Supple", "Hushed", "Carmine", "Ochre", "Umber", "Tawny", "Rusted",
        "Marbled", "Candid", "Burnished", "Fractured", "Cerulean", "Cloven", "Slanted",
        "Florid", "Lacquered", "Opaque", "Nomad", "Silvered", "Diaphanous", "Crestfallen",
        "Hush", "Reverie", "Sparse"
    };

    private static final String[] NOUNS = {
        "Tide", "Shore", "Veil", "Echo", "Ember", "Hush", "Drift", "Shade",
        "Reverie", "Bloom", "Mist", "Spark", "Hollow", "Ache", "Dusk", "Verse",
        "Petal", "Seam", "Lull", "Fog", "Rime", "Knell", "Pulse", "Rift",
        "Shroud", "Thorn", "Gale", "Brine", "Vale", "Trace", "Ebb", "Gloom",
        "Fable", "Sorrow", "Refrain", "Lament", "Solace", "Murmur", "Glow", "Stir",
        "Tempest", "Marrow", "Coil", "Chord", "Fracture", "Silence", "Ruin", "Fervor",
        "Hunger", "Vigil", "Weight", "Longing", "Absence", "Wonder", "Patience",
        "Threshold", "Current", "Vestige", "Cairn", "Flicker", "Plunge", "Tremble",
        "Wraith", "Canopy", "Meridian", "Brink", "Lore", "Signal", "Wound", "Vessel",
        "Cipher", "Toll", "Passage"
    };

    private final Random random = new Random();

    public String generate() {
        String base = ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " " +
                      NOUNS[random.nextInt(NOUNS.length)];

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
