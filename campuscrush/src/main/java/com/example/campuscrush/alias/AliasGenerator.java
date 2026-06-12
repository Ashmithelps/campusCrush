package com.example.campuscrush.alias;

import java.util.Random;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

@Component
public class AliasGenerator {

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

    /**
     * Generates a mask unique within the scope defined by {@code isTaken}
     * (e.g. one viewer's inbox). Masks are deliberately reusable across
     * scopes — global reuse strips the name of any stable identity.
     */
    public String generate(Predicate<String> isTaken) {
        String base = ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " " +
                      NOUNS[random.nextInt(NOUNS.length)];

        if (!isTaken.test(base)) {
            return base;
        }

        // Collision within scope — append a number until unique
        for (int i = 2; i <= 999; i++) {
            String candidate = base + " " + i;
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }

        // Fallback (effectively impossible within one inbox)
        return base + " " + System.currentTimeMillis();
    }
}
