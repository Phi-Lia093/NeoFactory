package com.philia093.neofactory.world;

import java.util.Locale;

/**
 * How a world is played.
 * <p>
 * The mode only changes what the player owns and how they get it: in survival they
 * collect what they break and craft, in creative they own every item of the game and
 * ask for it through the creative inventory, see
 * {@link com.philia093.neofactory.gui.CreativeInventoryGui}. Everything else - the
 * terrain, the machines and the save format - is the same in both modes.
 * <p>
 * The name is what a save file stores and what a player types after {@code /gamemode},
 * so it may never change once a world was written with it.
 */
public enum GameMode {

    /** The player collects what is around them, the default of every world. */
    SURVIVAL("survival"),

    /** The player owns every item of the game and takes it from the creative inventory. */
    CREATIVE("creative");

    /** Name the save file stores and a player types. */
    private final String name;

    GameMode(String name) {
        this.name = name;
    }

    /** Name the save file stores and a player types. */
    public String modeName() {
        return name;
    }

    /**
     * Looks a mode up by the name a player types.
     *
     * @param name name of the mode, upper and lower case mixed as the player likes
     * @return the mode, or {@code null} when the game has none of that name
     */
    public static GameMode byName(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        for (GameMode mode : values()) {
            if (mode.name.equals(key)) {
                return mode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
