package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
 * <b>A machine without a front names its sides the way the world does.</b> A box of cells is the same from
 * every side of it: it carries no mouth a player stands in front of, no side of it is the one that carries
 * nothing, and the words {@code LEFT}, {@code RIGHT} and {@code BACK} would have nothing to be read from, see
 * {@link #nameOf(BlockFace, BlockFace)} and {@code FaceConfig#withoutFront}. Such a machine is reached over
 * {@code NORTH}, {@code EAST}, {@code SOUTH}, {@code WEST}, {@code UP} and {@code DOWN} instead, which is the
 * name of a side of the world and needs no front to mean something.
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

    /**
     * Side a machine looks in before anybody turns it.
     * <p>
     * A machine is built with its front to the north and keeps that front until a wrench turns it, which is
     * also the front the default assignment of {@link FaceConfig} is written for: a player who built a
     * machine and never touched it reads the same names on its sides as a machine that was turned does.
     */
    public static final BlockFace DEFAULT_FRONT = BlockFace.NORTH;

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
        if (!isHorizontal(facing)) {
            throw new IllegalArgumentException("A machine looks along the horizon and not " + facing
                    + ", so it has no flanks at all");
        }
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
        if (!isHorizontal(facing)) {
            throw new IllegalArgumentException("A machine looks along the horizon and not " + facing
                    + ", so it has no flanks at all");
        }
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
     * @throws IllegalArgumentException when the machine does not look along the horizon
     */
    public static List<BlockFace> order(BlockFace facing) {
        if (!isHorizontal(facing)) {
            throw new IllegalArgumentException("A machine looks along the horizon and not " + facing
                    + ", so its front comes first of nothing");
        }
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
     * <p>
     * A machine that looks somewhere names its sides from that front, see the note on this class. <b>A machine
     * without a front - a box of cells - names the side by the side of the world it lies on</b>, because
     * {@code LEFT} and {@code BACK} are words about a front and such a machine has none, see
     * {@link #worldNameOf(BlockFace)}.
     *
     * @param facing side the machine looks in, {@code null} for a machine without a front
     * @param side side to name, {@code null} for a side that carries nothing
     * @return the name a player reads, {@link #NONE} for a side that carries nothing
     * @throws IllegalArgumentException when the side is no side of that machine
     */
    public static String nameOf(BlockFace facing, BlockFace side) {
        if (side == null) {
            return NONE;
        }
        if (facing == null) {
            return worldNameOf(side);
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
     * The name of a side of the world, which is what a machine without a front names its sides by.
     * <p>
     * The four sides of the horizon carry their own name - {@code NORTH}, {@code EAST}, {@code SOUTH} and
     * {@code WEST} - while the ceiling and the floor of the world are named the way every machine names them,
     * {@code UP} and {@code DOWN}, see {@link #UP} and {@link #DOWN}.
     *
     * @param side side of the world to name
     * @return the name a player reads
     */
    public static String worldNameOf(BlockFace side) {
        Objects.requireNonNull(side, "side");
        return switch (side) {
            case TOP -> UP;
            case BOTTOM -> DOWN;
            default -> side.name();
        };
    }

    /**
     * The side a name stands for.
     *
     * @param facing side the machine looks in, {@code null} for a machine without a front
     * @param name name such as {@code LEFT} or {@code NORTH}, in any case
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
