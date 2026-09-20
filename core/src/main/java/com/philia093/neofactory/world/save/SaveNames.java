package com.philia093.neofactory.world.save;

/**
 * Cleans the name of a save game before it is stored.
 * <p>
 * The name is not used as a file name: a save game lives in a folder named after
 * its identifier, and the readable name is written into the file. Renaming a world
 * therefore never touches the file system, and a name may hold characters that a
 * file system would refuse.
 * <p>
 * What is removed is what would look broken in the world list: control characters,
 * leading and trailing blanks and a name that is longer than the list can show.
 */
public final class SaveNames {

    /** Text used instead of an empty name. */
    private static final String FALLBACK = SaveFormat.DEFAULT_NAME;

    private SaveNames() {
        // Utility class: never instantiated.
    }

    /**
     * Returns a name that can be stored and shown.
     *
     * @param name requested name, may be {@code null}
     * @return the cleaned name, never empty
     */
    public static String sanitize(String name) {
        if (name == null) {
            return FALLBACK;
        }
        StringBuilder cleaned = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            // Control characters would break the layout of the list.
            if (character >= ' ') {
                cleaned.append(character);
            }
        }
        String result = cleaned.toString().trim();
        if (result.isEmpty()) {
            return FALLBACK;
        }
        if (result.length() > SaveFormat.MAX_NAME_LENGTH) {
            result = result.substring(0, SaveFormat.MAX_NAME_LENGTH).trim();
        }
        return result.isEmpty() ? FALLBACK : result;
    }

    /**
     * Creates an identifier for a new save game.
     * <p>
     * The identifier only uses letters and digits, so it is safe as a folder name on
     * every platform. A suffix is added when the identifier is already taken, which
     * keeps two worlds created in the same second apart.
     *
     * @param takenIds identifiers that are already in use
     * @return an unused identifier
     */
    public static String nextId(java.util.Collection<String> takenIds) {
        String base = Long.toString(System.currentTimeMillis(), 36);
        if (!takenIds.contains(base)) {
            return base;
        }
        for (int suffix = 2; suffix < 1000; suffix++) {
            String candidate = base + "_" + suffix;
            if (!takenIds.contains(candidate)) {
                return candidate;
            }
        }
        return base + "_" + System.nanoTime();
    }

    /**
     * Returns a name that is not used by another world yet.
     * <p>
     * Creating a world twice with the same name is allowed, a duplicate would only
     * make the list hard to read, so a number is appended instead.
     *
     * @param name requested name
     * @param takenNames names that are already in use
     * @return the cleaned name, made unique when necessary
     */
    public static String uniqueName(String name, java.util.Collection<String> takenNames) {
        String base = sanitize(name);
        if (!takenNames.contains(base)) {
            return base;
        }
        for (int suffix = 2; suffix < 1000; suffix++) {
            String candidate = base + " " + suffix;
            if (candidate.length() > SaveFormat.MAX_NAME_LENGTH) {
                candidate = base.substring(0,
                        Math.max(1, SaveFormat.MAX_NAME_LENGTH - 2)) + " " + suffix;
            }
            if (!takenNames.contains(candidate)) {
                return candidate;
            }
        }
        return base;
    }
}
