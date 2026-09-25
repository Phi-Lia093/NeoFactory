package com.philia093.neofactory.item;

import java.util.Locale;

/**
 * The tool a player holds while they work on a face of a block.
 * <p>
 * A block of the industry is worked on from the face it is looked at, and every face of it is reached
 * through a grid of nine cells, see {@link com.philia093.neofactory.world.interaction.FaceGrid}. Which
 * tool a player holds decides whether that grid appears and what a cell of it does: the wrench turns a
 * machine and breaks a connection, the wire cutter cuts a line, the crowbar takes a block apart, and a
 * screwdriver loosens what a screw holds. An empty hand shows the grid while the player crouches, which
 * is the one way to look at a block without doing anything to it.
 * <p>
 * The tool is a property of the item and not of the stack, so an item says once and for all what it is
 * for, see {@link Item#faceTool()}. A tool that wears out, a material and a plain block therefore all
 * answer {@link #NONE}: they are not held at a face but used on it.
 */
public enum FaceTool {

    /** Nothing is held that addresses a face. */
    NONE,

    /** An empty hand of a crouching player, which shows the grid and changes nothing. */
    EMPTY_HAND,

    /** The wrench, the tool that turns a machine and opens a pipe. */
    WRENCH,

    /** The wire cutter, the tool that cuts a line. */
    WIRE_CUTTER,

    /** The crowbar, the tool that takes a block apart. */
    CROWBAR,

    /** The screwdriver, the tool that loosens what a screw holds. */
    SCREWDRIVER;

    /**
     * The tool a player addresses a face with.
     *
     * @param held stack the player holds, may be empty
     * @param crouching {@code true} while the player crouches
     * @return the tool, or {@link #NONE} when nothing here addresses a face
     */
    public static FaceTool of(ItemStack held, boolean crouching) {
        if (held != null && !held.isEmpty()) {
            FaceTool tool = held.item().faceTool();
            return tool == null ? NONE : tool;
        }
        return crouching ? EMPTY_HAND : NONE;
    }

    /** {@code true} when this tool addresses a face at all, so the grid may appear. */
    public boolean addressesAFace() {
        return this != NONE;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
