package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.interaction.FaceRole;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.ArrayList;
import java.util.List;

/**
 * What a player made of the sides of a machine: the side each of its parts is reached through.
 * <p>
 * A machine moves items, fluids and the power of a line of cables, and every one of those parts is reached
 * through exactly one side of its block: the tank of water is filled through one side, the tank of steam is
 * emptied through another and the plug the power leaves through is a third. This class is that assignment -
 * and it is the place all the rules of it live, so that the tank of a machine, the line of pipes next to it,
 * the grid of faces and the interface of the machine all read the same answer.
 * <p>
 * <b>A side carries one job at a time.</b> Giving a side a job takes the job that stood there away, which
 * is what makes a machine usable: a player who sets the plug of the power onto the side a pipe was built
 * against has not built two things into one cell, they have moved the pipe away. The tank that lost its
 * side reports {@code FACING: NONE} until the player gives it another one, and the rule is applied here and
 * not by the player.
 * <p>
 * <b>The front carries nothing at all.</b> The side a machine looks in shows the machine itself and no pipe,
 * cable or belt is ever built against it, so {@link FaceRole#NONE} is the only job it may answer with: the
 * side is refused when a player tries to set a job on the front and it loses whatever job it had when the
 * machine is turned onto it, see {@link #turned(BlockFace)}.
 * <p>
 * <b>Which jobs a machine has is read from the machine itself.</b> A tank of the machine gives a side that
 * carries a fluid, the buffer of the machine gives the plugs: a machine whose buffer may be filled takes
 * power in, one whose buffer may be emptied gives power out, and a machine that blows its spent steam out
 * gives an exhaust. A machine that holds no tank of a kind and no such buffer has no side of it - there is
 * nothing to configure, which is why the shape of the sides is built once in the constructor from the parts
 * of the machine and never asks a screen or a block.
 */
public final class FaceConfig {

    /** Key one plug is stored under inside the group of the sides. */
    private static final String ENERGY_IN = "EnergyIn";

    /** Key the other plug is stored under. */
    private static final String ENERGY_OUT = "EnergyOut";

    /** Key the vent of the steam is stored under. */
    private static final String EXHAUST = "Exhaust";

    /** Start of the keys the tanks are stored under, one key per tank. */
    private static final String TANK = "Tank";

    /** Role of every tank of the machine, in the order {@link Machine#tank(int)} names them. */
    private final MachineTank.Role[] tankRoles;

    /** {@code true} when the power of a line may enter this machine, which gives it a plug. */
    private final boolean takesPower;

    /** {@code true} when the power this machine makes leaves it through a plug. */
    private final boolean givesPower;

    /** {@code true} when this machine blows its spent steam out of a side. */
    private boolean blowsSteam;

    /** Side of each tank of the machine, {@code null} for a tank that is reached from nowhere. */
    private final BlockFace[] tanks;

    private BlockFace energyIn;
    private BlockFace energyOut;
    private BlockFace exhaust;

    /**
     * Creates the sides of a machine and puts them where a machine of that shape starts.
     *
     * @param tankRoles role of every tank of the machine, in the order the machine holds them
     * @param energy buffer of the machine, asked which way it may be used
     */
    public FaceConfig(MachineTank.Role[] tankRoles, EnergyStorage energy) {
        this.tankRoles = tankRoles.clone();
        this.tanks = new BlockFace[tankRoles.length];
        this.takesPower = energy.capacity() > 0 && energy.canReceive();
        this.givesPower = energy.capacity() > 0 && energy.canExtract();
        defaultSides();
    }

    /**
     * Gives the sides of this machine a vent for its spent steam.
     * <p>
     * Called by a machine of the age of steam while it is created - the machine that knows it breathes
     * rather than a question asked of it - and it puts the vent on the side the back of a machine that looks
     * north lies on, see {@link #defaultSides()}.
     *
     * @return this configuration, for a machine that builds it in one expression
     */
    public FaceConfig withExhaust() {
        blowsSteam = true;
        if (exhaust == null) {
            exhaust = BlockFace.SOUTH;
        }
        return this;
    }

    /**
     * Puts the sides where a machine that nobody has touched starts, with its front to the north.
     * <p>
     * The sides are named as the machine stands when it is built - its front to the north, see
     * {@link MachineSides#DEFAULT_FRONT} - and every part of it is put on a side of its own, named the way a
     * player who stands in front of the machine reads it: the first tank that is filled takes the right flank,
     * the first tank that is emptied the left one, the vent of the steam and the plug that takes power the
     * back, and the plug that gives power the left flank. A second tank of a kind takes the ceiling or the
     * floor, and a third is reached from nowhere until a player gives it a side. A machine whose front is
     * turned afterwards keeps those sides, so a player always meets the same machine, see
     * {@link #turned(BlockFace)}.
     */
    private void defaultSides() {
        int filled = 0;
        int emptied = 0;
        for (int index = 0; index < tanks.length; index++) {
            tanks[index] = tankRoles[index] == MachineTank.Role.INPUT
                    ? filledDefault(filled++)
                    : emptiedDefault(emptied++);
        }
        if (blowsSteam) {
            exhaust = BlockFace.SOUTH;
        }
        if (takesPower) {
            energyIn = BlockFace.SOUTH;
        }
        if (givesPower) {
            energyOut = BlockFace.EAST;
        }
    }

    /** Side the n-th tank that is filled starts on. */
    private static BlockFace filledDefault(int nth) {
        return switch (nth) {
            case 0 -> BlockFace.WEST;
            case 1 -> BlockFace.TOP;
            default -> null;
        };
    }

    /** Side the n-th tank that is emptied starts on. */
    private static BlockFace emptiedDefault(int nth) {
        return switch (nth) {
            case 0 -> BlockFace.EAST;
            case 1 -> BlockFace.BOTTOM;
            default -> null;
        };
    }

    /** Amount of tanks this machine holds, which is how many sides it has to offer them. */
    public int tankCount() {
        return tanks.length;
    }

    /** {@code true} when the power of a line may enter this machine. */
    public boolean takesPower() {
        return takesPower;
    }

    /** {@code true} when the power this machine makes leaves it through a side. */
    public boolean givesPower() {
        return givesPower;
    }

    /** {@code true} when this machine blows its spent steam out of a side. */
    public boolean blowsSteam() {
        return blowsSteam;
    }

    /** Side the power of the line enters this machine through, {@code null} for no plug. */
    public BlockFace energyIn() {
        return energyIn;
    }

    /** Side the power this machine makes leaves it through, {@code null} for no plug. */
    public BlockFace energyOut() {
        return energyOut;
    }

    /** Side this machine blows its spent steam out of, {@code null} for a machine that breathes not. */
    public BlockFace exhaust() {
        return exhaust;
    }

    /**
     * Side one tank is reached through.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @return the side, or {@code null} when nothing reaches that tank
     */
    public BlockFace faceOfTank(int index) {
        return tanks[index];
    }

    /** Role of one tank of this machine, which decides whether a side of it takes or gives. */
    public MachineTank.Role roleOfTank(int index) {
        return tankRoles[index];
    }

    /**
     * Gives the power of the line a side of the machine of its own.
     *
     * @param face side the power enters through, {@code null} to take the plug away
     * @param facing side the machine looks in, which carries nothing
     * @return {@code true} when the sides changed
     */
    public boolean setEnergyIn(BlockFace face, BlockFace facing) {
        if (!takesPower || face == facing) {
            return false;
        }
        boolean changed = face != energyIn || owners(face) > 1;
        release(face);
        energyIn = face;
        return changed;
    }

    /**
     * Gives the power this machine makes a side of the machine of its own.
     *
     * @param face side the power leaves through, {@code null} to take the plug away
     * @param facing side the machine looks in, which carries nothing
     * @return {@code true} when the sides changed
     */
    public boolean setEnergyOut(BlockFace face, BlockFace facing) {
        if (!givesPower || face == facing) {
            return false;
        }
        boolean changed = face != energyOut || owners(face) > 1;
        release(face);
        energyOut = face;
        return changed;
    }

    /**
     * Gives the spent steam of the machine a side of its own.
     *
     * @param face side the steam blows out of, {@code null} for a machine that has no vent
     * @param facing side the machine looks in, which carries nothing
     * @return {@code true} when the sides changed
     */
    public boolean setExhaust(BlockFace face, BlockFace facing) {
        if (!blowsSteam || face == facing) {
            return false;
        }
        boolean changed = face != exhaust || owners(face) > 1;
        release(face);
        exhaust = face;
        return changed;
    }

    /**
     * Gives one tank of the machine a side of its own.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @param face side the tank is reached through, {@code null} for a tank nothing reaches
     * @param facing side the machine looks in, which carries nothing
     * @return {@code true} when the sides changed
     */
    public boolean setTank(int index, BlockFace face, BlockFace facing) {
        if (index < 0 || index >= tanks.length || face == facing) {
            return false;
        }
        boolean changed = face != tanks[index] || owners(face) > 1;
        release(face);
        tanks[index] = face;
        return changed;
    }

    /**
     * Takes every job away from the side a machine was turned onto.
     * <p>
     * The front of a machine carries nothing, so the side that becomes the front loses whatever it had: a
     * machine that is turned onto the side a pipe was built against leaves the pipe behind with a mouth
     * that reaches nothing, which is the rule of the front and of every other side of the machine.
     *
     * @param facing side the machine now looks in
     * @return {@code true} when a job was taken away
     */
    public boolean turned(BlockFace facing) {
        if (facing == null) {
            return false;
        }
        return release(facing);
    }

    /**
     * Takes a job away from every part of the machine that has it.
     *
     * @param face side to clear, {@code null} does nothing
     * @return {@code true} when a job was taken away
     */
    private boolean release(BlockFace face) {
        if (face == null) {
            return false;
        }
        boolean cleared = false;
        if (energyIn == face) {
            energyIn = null;
            cleared = true;
        }
        if (energyOut == face) {
            energyOut = null;
            cleared = true;
        }
        if (exhaust == face) {
            exhaust = null;
            cleared = true;
        }
        for (int index = 0; index < tanks.length; index++) {
            if (tanks[index] == face) {
                tanks[index] = null;
                cleared = true;
            }
        }
        return cleared;
    }

    /** How many parts of the machine are reached through one side. */
    private int owners(BlockFace face) {
        if (face == null) {
            return 0;
        }
        int count = 0;
        if (energyIn == face) {
            count++;
        }
        if (energyOut == face) {
            count++;
        }
        if (exhaust == face) {
            count++;
        }
        for (BlockFace tank : tanks) {
            if (tank == face) {
                count++;
            }
        }
        return count;
    }

    /**
     * The job one side of a machine carries.
     *
     * @param face side of the machine
     * @param facing side the machine looks in
     * @return the job of that side, {@link FaceRole#NONE} for a side that carries nothing
     */
    public FaceRole roleOn(BlockFace face, BlockFace facing) {
        if (face == null || face == facing) {
            return FaceRole.NONE;
        }
        if (face == energyIn) {
            return FaceRole.ENERGY_IN;
        }
        if (face == energyOut) {
            return FaceRole.ENERGY_OUT;
        }
        if (face == exhaust) {
            return FaceRole.EXHAUST;
        }
        for (int index = 0; index < tanks.length; index++) {
            if (tanks[index] == face) {
                return tankRoles[index] == MachineTank.Role.INPUT
                        ? FaceRole.FLUID_IN
                        : FaceRole.FLUID_OUT;
            }
        }
        return FaceRole.NONE;
    }

    /**
     * Walks one tank of the machine to its next side.
     * <p>
     * The wheel of the interface walks the sides a player reads - none, and then the five sides the front
     * leaves - and <b>it steps over what another part of the machine owns</b>: the plug of the power is not
     * taken away by a wheel that runs over a tank. Whether the tank is stepped forward or back is what the
     * sign of the step says, and a tank that carries nothing starts at the first side of the walk.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @param step notches to walk, positive or negative
     * @param facing side the machine looks in
     * @return {@code true} when the side of that tank changed
     */
    public boolean cycleTank(int index, int step, BlockFace facing) {
        if (index < 0 || index >= tanks.length || step == 0) {
            return false;
        }
        List<BlockFace> walk = new ArrayList<>();
        walk.add(null);
        for (BlockFace side : MachineSides.order(facing)) {
            if (side != facing && (owners(side) == 0 || side == tanks[index])) {
                walk.add(side);
            }
        }
        int at = walk.indexOf(tanks[index]);
        BlockFace wanted = walk.get(Math.floorMod((at < 0 ? 0 : at) + step, walk.size()));
        if (wanted == tanks[index]) {
            return false;
        }
        tanks[index] = wanted;
        return true;
    }

    /**
     * Writes the sides into the group a machine is stored in.
     *
     * @param data group of the machine
     */
    public void save(NbtCompound data) {
        NbtCompound faces = new NbtCompound(SaveTags.MACHINE_FACES);
        faces.putString(ENERGY_IN, name(energyIn));
        faces.putString(ENERGY_OUT, name(energyOut));
        faces.putString(EXHAUST, name(exhaust));
        for (int index = 0; index < tanks.length; index++) {
            faces.putString(TANK + index, name(tanks[index]));
        }
        data.put(faces);
    }

    /**
     * Reads the sides a machine was stored with.
     * <p>
     * A machine that was never touched was stored without a group of sides, and a machine of a world that is
     * older than this group keeps the sides it was built with: a key that is not there leaves that part of
     * the machine where {@link #defaultSides()} put it, while a side that was stored as {@code NONE} really
     * is no side at all.
     *
     * @param data group of the machine
     */
    public void load(NbtCompound data) {
        NbtCompound faces = data.getCompound(SaveTags.MACHINE_FACES);
        if (faces == null) {
            return;
        }
        if (faces.contains(ENERGY_IN)) {
            energyIn = stored(faces, ENERGY_IN);
        }
        if (faces.contains(ENERGY_OUT)) {
            energyOut = stored(faces, ENERGY_OUT);
        }
        if (faces.contains(EXHAUST)) {
            exhaust = stored(faces, EXHAUST);
        }
        for (int index = 0; index < tanks.length; index++) {
            if (faces.contains(TANK + index)) {
                tanks[index] = stored(faces, TANK + index);
            }
        }
    }

    /** Name a side is stored under, empty for a side that carries nothing. */
    private static String name(BlockFace face) {
        return face == null ? "" : face.name();
    }

    /** Side a group holds under a key, {@code null} for a key that names no side. */
    private static BlockFace stored(NbtCompound faces, String key) {
        return BlockFace.byName(faces.getString(key, ""));
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("FaceConfig(");
        text.append("power in ").append(energyIn).append(", power out ").append(energyOut)
                .append(", exhaust ").append(exhaust);
        for (int index = 0; index < tanks.length; index++) {
            text.append(", ").append(tankRoles[index]).append(' ').append(index).append(' ')
                    .append(tanks[index]);
        }
        return text.append(')').toString();
    }
}
