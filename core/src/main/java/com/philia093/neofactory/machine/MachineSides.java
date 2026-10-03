package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The names a player reads for the sides of a machine, and the order the wheel walks them in.
 * <p>
 * A machine has a front - the side its art shows, the side a player who built it is looking at - and the
 * sides of it are named from there: {@code FRONT} is the side it looks in, {@code BACK} the one behind,
 * {@code UP} and {@code DOWN} are the ceiling and the floor, and {@code LEFT} and {@code RIGHT} are the two
 * flanks as a player standing in front of the machine sees them. <b>The names are what the interface
 * writes</b>: the tooltip of a tank says {@code FACING: LEFT} and never {@code north}, because a player who
 * turned a machine wants to read where a side lies on the machine and not where it lies in the world.
 * <p>
 * <b>A machine looks north, east, south or west and never up or down.</b> The four names around the front
 * only mean something while the front is horizontal - a machine that looked at the ceiling would have no
 * left flank - so a machine that is turned keeps to the four sides of the horizon, see
 * {@code MachineBlockEntity#operateFace}.
 * <p>
 * <b>The order is the one a player reads.</b> {@link #order(BlockFace)} lists the six sides the way the
 * wheel of the interface walks them, which is also the order the tooltip of a tank names them in.
 */
public final class MachineSides {

    /** Name of a side that carries nothing. */
    public static final String NONE = "NONE";

    /** Name of the side a machine looks in, the one that carries no job at all. */
    public static final String FRONT = "FRONT";

    /** Name of the side behind the front. */
    public static final String BACK = "BACK";

    /** Name of the ceiling of a machine. */
    public static final String UP = "UP";

    /** Name of the floor of a machine. */
    public static final String DOWN = "DOWN";

    /** Name of the flank to the left of a player who stands in front of the machine. */
    public static final String LEFT = "LEFT";

    /** Name of the flank to the right of that player. */
    public static final String RIGHT = "RIGHT";

    private MachineSides() {
        // Utility class: never instantiated.
    }

    /**
     * {@code true} when this side may be the front of a machine.
     * <p>
     * The four names around a front only mean something while the front is horizontal, so a machine that is
     * turned is turned to one of the four sides of the horizon.
     *
     * @param facing side a machine would look in
     * @return {@code true} when a machine may look that way
     */
    public static boolean isHorizontal(BlockFace facing) {
        return facing != null && facing.y() == 0;
    }

    /**
     * The side to the left of a machine, seen by a player who stands in front of it.
     * <p>
     * A player in front of a machine that looks north looks south themselves, and their left hand points
     * east, so east is the left flank of that machine.
     *
     * @param facing side the machine looks in
     * @return the side that lies to the left of the front
     * @throws IllegalArgumentException when the machine does not look along the horizon
     */
    public static BlockFace leftOf(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> throw new IllegalArgumentException("A machine looks along the horizon and not "
                    + facing);
        };
    }

    /**
     * The side to the right of a machine, seen by a player who stands in front of it.
     *
     * @param facing side the machine looks in
     * @return the side that lies to the right of the front
     * @throws IllegalArgumentException when the machine does not look along the horizon
     */
    public static BlockFace rightOf(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST;
            case EAST -> BlockFace.NORTH;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> throw new IllegalArgumentException("A machine looks along the horizon and not "
                    + facing);
        };
    }

    /**
     * The six sides of a machine in the order a player reads them.
     *
     * @param facing side the machine looks in
     * @return the sides, the front first
     */
    public static List<BlockFace> order(BlockFace facing) {
        List<BlockFace> sides = new ArrayList<>(BlockFace.ALL.length);
        sides.add(facing);
        sides.add(facing.opposite());
        sides.add(BlockFace.TOP);
        sides.add(BlockFace.BOTTOM);
        sides.add(leftOf(facing));
        sides.add(rightOf(facing));
        return List.copyOf(sides);
    }

    /**
     * The name of one side of a machine.
     *
     * @param facing side the machine looks in
     * @param side side to name, {@code null} for a side that carries nothing
     * @return the name a player reads, {@link #NONE} for a side that carries nothing
     * @throws IllegalArgumentException when the side is no side of that machine
     */
    public static String nameOf(BlockFace facing, BlockFace side) {
        if (side == null) {
            return NONE;
        }
        if (side == facing) {
            return FRONT;
        }
        if (side == facing.opposite()) {
            return BACK;
        }
        if (side == BlockFace.TOP) {
            return UP;
        }
        if (side == BlockFace.BOTTOM) {
            return DOWN;
        }
        if (side == leftOf(facing)) {
            return LEFT;
        }
        if (side == rightOf(facing)) {
            return RIGHT;
        }
        throw new IllegalArgumentException("The " + side + " is no side of a machine that looks " + facing);
    }

    /**
     * The side a name stands for.
     *
     * @param facing side the machine looks in
     * @param name name such as {@code LEFT}, in any case
     * @return the side, or {@code null} for {@link #NONE} or a name no side of a machine carries
     */
    public static BlockFace sideOf(BlockFace facing, String name) {
        if (name == null) {
            return null;
        }
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        for (BlockFace side : BlockFace.ALL) {
            if (nameOf(facing, side).equals(wanted)) {
                return side;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "MachineSides(FRONT, BACK, UP, DOWN, LEFT, RIGHT)";
    }
}
