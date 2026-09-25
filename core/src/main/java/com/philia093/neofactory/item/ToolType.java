package com.philia093.neofactory.item;

import java.util.Locale;

/**
 * Kind of tool a block is worked with.
 * <p>
 * Two questions are answered by the kind of a tool and not by the material it is made of: which
 * blocks it mines quickly, and which blocks need exactly it before they hand their item over. A
 * stone of level {@code 1} wants a pickaxe, so an axe breaks it - slowly and leaving nothing
 * behind, see {@code HardnessMining} - while a block that asks for no tool at all is worked by
 * every kind alike, only faster with the one that fits it.
 * <p>
 * The shape a block names and the shape a tool names are compared as objects, so a block that
 * wants an axe and a tool that is an axe meet without any name in between.
 * <p>
 * A new kind - the mortar of a chemist, the screwdriver of a workshop - is a value here and a
 * picture, nothing else. Whether a tool <i>wears out</i> is decided by its item and not by its
 * kind, see {@link Damageable}: a mortar wears out like a pickaxe does, and a kind of its own is
 * all it needs to be mined with.
 */
public enum ToolType {

    /** Chisel for the stone family, the ores and everything built of stone. */
    PICKAXE,

    /** Blade for wood: trunks, boards, tables and ladders. */
    AXE,

    /** Blade for earth: soil, sand, gravel and clay. */
    SHOVEL,

    /** Blade for the field: a canopy and, one day, the ground of a farm. */
    HOE,

    /** Blade of a fight: it cuts what stands and mines nothing, which is why no block names it. */
    SWORD;

    /**
     * Name of this kind as a word, used where a tool names what it is.
     *
     * @return the name with a capital letter, such as {@code "Pickaxe"}
     */
    public String displayName() {
        String name = name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
