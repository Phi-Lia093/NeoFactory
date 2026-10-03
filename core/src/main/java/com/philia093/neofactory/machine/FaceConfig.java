package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.interaction.FaceRole;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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
 * <b>A side is named from the front of the machine.</b> What a part of a machine is set to is one of the words
 * a player reads at its block - {@code RIGHT}, {@code LEFT}, {@code UP}, {@code DOWN} and {@code BACK}, see
 * {@link MachineSides} - and never a side of the world: <b>the sides of a machine travel with the machine</b>,
 * so a tank a player set to the right flank is reached through that flank whatever the front of the machine is
 * turned to, and a line that was built towards a machine keeps reaching it. That is also what keeps the front
 * of a machine out of it: {@code FRONT} is no word a part is set to, so no side of a machine ever covers the
 * face the machine shows.
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

    /** Sides the tanks that are filled start on, in the words a player reads. */
    private static final String[] FILLED_DEFAULTS = {MachineSides.RIGHT, MachineSides.UP};

    /** Sides the tanks that are emptied start on, in the words a player reads. */
    private static final String[] EMPTIED_DEFAULTS = {MachineSides.LEFT, MachineSides.DOWN};

    /** Side the vent of a machine of steam and the plug it takes power in through start on. */
    private static final String BACK_DEFAULT = MachineSides.BACK;

    /** Side the plug a machine gives its power out through starts on. */
    private static final String OUT_DEFAULT = MachineSides.LEFT;

    /**
     * The words a part of a machine is walked through by the wheel of its screen, in the order a player reads
     * them: nothing at all first, then the back of the machine, then its ceiling and its floor, and then the
     * two flanks.
     */
    private static final List<String> WALK = List.of(MachineSides.NONE, MachineSides.BACK, MachineSides.UP,
            MachineSides.DOWN, MachineSides.LEFT, MachineSides.RIGHT);

    /** Role of every tank of the machine, in the order {@link Machine#tank(int)} names them. */
    private final MachineTank.Role[] tankRoles;

    /** {@code true} when the power of a line may enter this machine, which gives it a plug. */
    private final boolean takesPower;

    /** {@code true} when the power this machine makes leaves it through a plug. */
    private final boolean givesPower;

    /** {@code true} when this machine blows its spent steam out of a side. */
    private boolean blowsSteam;

    /** Side of each tank in the words a player reads, {@code null} for a tank nothing reaches. */
    private final String[] tanks;

    /** Side the power of a line comes in through, {@code null} for a machine that takes none. */
    private String energyIn;

    /** Side the power this machine makes leaves through, {@code null} for a machine that gives none. */
    private String energyOut;

    /** Side the spent steam blows out of, {@code null} for a machine that breathes not. */
    private String exhaust;

    /** Side of the world the front of this machine looks in, which every word above is read from. */
    private BlockFace facing = MachineSides.DEFAULT_FRONT;

    /**
     * Creates the sides of a machine and puts them where a machine of that shape starts.
     *
     * @param tankRoles role of every tank of the machine, in the order the machine holds them
     * @param energy buffer of the machine, asked which way it may be used
     */
    public FaceConfig(MachineTank.Role[] tankRoles, EnergyStorage energy) {
        this.tankRoles = tankRoles.clone();
        this.tanks = new String[tankRoles.length];
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
            exhaust = BACK_DEFAULT;
        }
        return this;
    }

    /**
     * Puts the sides where a machine that nobody has touched starts.
     * <p>
     * The words below are the ones a player reads at the machine and not sides of the world, so the table is
     * the same for every machine whatever way it is built and whichever way its front is turned afterwards:
     * the first tank that is filled takes the right flank, a second one the ceiling, the first tank that is
     * emptied the left flank and a second one the floor, the vent of the steam and the plug that takes power
     * the back, and the plug that gives power the left flank. A third tank of a kind is reached from nowhere
     * until a player gives it a side, see {@link #cycleTank(int, int)}.
     */
    private void defaultSides() {
        int filled = 0;
        int emptied = 0;
        for (int index = 0; index < tanks.length; index++) {
            tanks[index] = tankRoles[index] == MachineTank.Role.INPUT
                    ? nth(FILLED_DEFAULTS, filled++)
                    : nth(EMPTIED_DEFAULTS, emptied++);
        }
        if (blowsSteam) {
            exhaust = BACK_DEFAULT;
        }
        if (takesPower) {
            energyIn = BACK_DEFAULT;
        }
        if (givesPower) {
            energyOut = OUT_DEFAULT;
        }
    }

    /** The side the n-th tank of a kind starts on, {@code null} for a tank beyond the table. */
    private static String nth(String[] table, int index) {
        return index < table.length ? table[index] : null;
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

    /** Side of the world the front of this machine looks in, which every word above is read from. */
    public BlockFace facing() {
        return facing;
    }

    /**
     * Says which way the front of this machine looks.
     * <p>
     * Called by the block entity of the machine while it is placed and while it is turned: the words of this
     * assignment are read from the front, so a machine that is turned keeps every side a player gave it - the
     * tank that was reached over the right flank is reached over the right flank of the new front as well -
     * and no side of it can ever cover the face the machine shows, see {@link MachineSides}.
     *
     * @param facing side of the world the front looks in, one of the four sides of the horizon
     */
    public void facing(BlockFace facing) {
        if (MachineSides.isHorizontal(facing)) {
            this.facing = facing;
        }
    }

    /** Side of the world the power of a line enters this machine through, {@code null} for no plug. */
    public BlockFace energyIn() {
        return sideOf(energyIn);
    }

    /** Side of the world the power this machine makes leaves it through, {@code null} for no plug. */
    public BlockFace energyOut() {
        return sideOf(energyOut);
    }

    /** Side the vent of this machine looks at, {@code null} for a machine that breathes not. */
    public BlockFace exhaust() {
        return sideOf(exhaust);
    }

    /**
     * Side of the world one tank is reached through.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @return the side, or {@code null} when nothing reaches that tank
     */
    public BlockFace faceOfTank(int index) {
        return sideOf(tanks[index]);
    }

    /**
     * Name of the side one tank is reached through, in the words a player reads.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @return the name, {@link MachineSides#NONE} for a tank nothing reaches
     */
    public String nameOfTank(int index) {
        return word(tanks[index]);
    }

    /** Name of the side the power of a line comes in through, in the words a player reads. */
    public String nameOfEnergyIn() {
        return word(energyIn);
    }

    /** Name of the side the power of this machine leaves through, in the words a player reads. */
    public String nameOfEnergyOut() {
        return word(energyOut);
    }

    /** Name of the side the spent steam blows out of, in the words a player reads. */
    public String nameOfExhaust() {
        return word(exhaust);
    }

    /** Side of the world a word names, {@code null} for a part that nothing reaches. */
    private BlockFace sideOf(String name) {
        return MachineSides.sideOf(facing, name);
    }

    /** The word a name is written with, {@link MachineSides#NONE} for a part that nothing reaches. */
    private static String word(String name) {
        return name == null ? MachineSides.NONE : name;
    }

    /** Role of one tank of this machine, which decides whether a side of it takes or gives. */
    public MachineTank.Role roleOfTank(int index) {
        return tankRoles[index];
    }

    /**
     * Gives the power of the line a side of the machine of its own.
     *
     * @param face side of the world the power enters through, {@code null} to take the plug away
     * @return {@code true} when the sides changed
     */
    public boolean setEnergyIn(BlockFace face) {
        if (!takesPower || face == facing) {
            return false;
        }
        return assign(face, name -> energyIn = name, energyIn);
    }

    /**
     * Gives the power this machine makes a side of the machine of its own.
     *
     * @param face side of the world the power leaves through, {@code null} to take the plug away
     * @return {@code true} when the sides changed
     */
    public boolean setEnergyOut(BlockFace face) {
        if (!givesPower || face == facing) {
            return false;
        }
        return assign(face, name -> energyOut = name, energyOut);
    }

    /**
     * Gives the spent steam of the machine a side of its own.
     *
     * @param face side of the world the steam blows out of, {@code null} for a machine that has no vent
     * @return {@code true} when the sides changed
     */
    public boolean setExhaust(BlockFace face) {
        if (!blowsSteam || face == facing) {
            return false;
        }
        return assign(face, name -> exhaust = name, exhaust);
    }

    /**
     * Gives one tank of the machine a side of its own.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @param face side of the world the tank is reached through, {@code null} for a tank nothing reaches
     * @return {@code true} when the sides changed
     */
    public boolean setTank(int index, BlockFace face) {
        if (index < 0 || index >= tanks.length || face == facing) {
            return false;
        }
        return assign(face, name -> tanks[index] = name, tanks[index]);
    }

    /**
     * Puts one part of the machine on the side a click named and takes the job of that side from whoever has
     * it, which is the rule of one job to a side.
     *
     * @param face side of the world that was clicked, {@code null} for the side the wheel walked to
     * @param setter what the part is set to
     * @param held the word the part carried until now
     * @return {@code true} when the sides changed
     */
    private boolean assign(BlockFace face, Consumer<String> setter, String held) {
        String name = face == null ? null : relative(face);
        if (name != null && name.equals(held)) {
            // The click landed on the side the part already carries: it stays there and nobody loses a side.
            return false;
        }
        boolean moved = !Objects.equals(name, held);
        boolean taken = release(name);
        setter.accept(name);
        return moved || taken;
    }

    /** The word a side of the world stands for, {@code null} for the front of the machine. */
    private String relative(BlockFace face) {
        String name = MachineSides.nameOf(facing, face);
        // The front of a machine carries nothing: a side that is the front is no side a part may be set to.
        return MachineSides.FRONT.equals(name) ? null : name;
    }

    /**
     * Takes a job away from every part of the machine that has it.
     *
     * @param name side to clear, {@code null} does nothing
     * @return {@code true} when a job was taken away
     */
    private boolean release(String name) {
        if (name == null) {
            return false;
        }
        boolean cleared = false;
        if (name.equals(energyIn)) {
            energyIn = null;
            cleared = true;
        }
        if (name.equals(energyOut)) {
            energyOut = null;
            cleared = true;
        }
        if (name.equals(exhaust)) {
            exhaust = null;
            cleared = true;
        }
        for (int index = 0; index < tanks.length; index++) {
            if (name.equals(tanks[index])) {
                tanks[index] = null;
                cleared = true;
            }
        }
        return cleared;
    }

    /** How many parts of the machine are reached through one side. */
    private int owners(String name) {
        if (name == null) {
            return 0;
        }
        int count = 0;
        if (name.equals(energyIn)) {
            count++;
        }
        if (name.equals(energyOut)) {
            count++;
        }
        if (name.equals(exhaust)) {
            count++;
        }
        for (String tank : tanks) {
            if (name.equals(tank)) {
                count++;
            }
        }
        return count;
    }

    /**
     * The job one side of a machine carries.
     *
     * @param face side of the world
     * @return the job of that side, {@link FaceRole#NONE} for a side that carries nothing
     */
    public FaceRole roleOn(BlockFace face) {
        if (face == null || face == facing) {
            return FaceRole.NONE;
        }
        String name = relative(face);
        if (name == null) {
            return FaceRole.NONE;
        }
        if (name.equals(energyIn)) {
            return FaceRole.ENERGY_IN;
        }
        if (name.equals(energyOut)) {
            return FaceRole.ENERGY_OUT;
        }
        if (name.equals(exhaust)) {
            return FaceRole.EXHAUST;
        }
        for (int index = 0; index < tanks.length; index++) {
            if (name.equals(tanks[index])) {
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
     * @return {@code true} when the side of that tank changed
     */
    public boolean cycleTank(int index, int step) {
        if (index < 0 || index >= tanks.length || step == 0) {
            return false;
        }
        List<String> walk = new ArrayList<>();
        for (String side : WALK) {
            if (MachineSides.NONE.equals(side) || owners(side) == 0 || side.equals(tanks[index])) {
                walk.add(side);
            }
        }
        String held = word(tanks[index]);
        int at = walk.indexOf(held);
        String wanted = walk.get(Math.floorMod((at < 0 ? 0 : at) + step, walk.size()));
        if (wanted.equals(held)) {
            return false;
        }
        tanks[index] = MachineSides.NONE.equals(wanted) ? null : wanted;
        return true;
    }

    /**
     * Writes the sides into the group a machine is stored in.
     *
     * @param data group of the machine
     */
    /**
     * Writes the sides into the group a machine is stored in.
     * <p>
     * What is written is the words a player reads - {@code RIGHT}, {@code BACK} and the rest - and never a side
     * of the world, so a stored machine keeps its sides when its front is turned, see {@link #facing(BlockFace)}.
     *
     * @param data group of the machine
     */
    public void save(NbtCompound data) {
        NbtCompound faces = new NbtCompound(SaveTags.MACHINE_FACES);
        faces.putString(ENERGY_IN, word(energyIn));
        faces.putString(ENERGY_OUT, word(energyOut));
        faces.putString(EXHAUST, word(exhaust));
        for (int index = 0; index < tanks.length; index++) {
            faces.putString(TANK + index, word(tanks[index]));
        }
        data.put(faces);
    }

    /**
     * Reads the sides a machine was stored with.
     * <p>
     * A machine that was never touched was stored without a group of sides, and a machine of a world that is
     * older than this group keeps the sides it was built with: a key that is not there leaves that part of
     * the machine where {@link #defaultSides()} put it, while a side that was stored as {@code NONE} really
     * is no side at all. A word that names no side of a machine - a side of the world, as they were stored
     * before the sides became words - is read as no side either.
     *
     * @param data group of the machine
     */
    public void load(NbtCompound data) {
        NbtCompound faces = data.getCompound(SaveTags.MACHINE_FACES);
        if (faces == null) {
            return;
        }
        if (faces.contains(ENERGY_IN)) {
            energyIn = stored(faces.getString(ENERGY_IN, ""));
        }
        if (faces.contains(ENERGY_OUT)) {
            energyOut = stored(faces.getString(ENERGY_OUT, ""));
        }
        if (faces.contains(EXHAUST)) {
            exhaust = stored(faces.getString(EXHAUST, ""));
        }
        for (int index = 0; index < tanks.length; index++) {
            if (faces.contains(TANK + index)) {
                tanks[index] = stored(faces.getString(TANK + index, ""));
            }
        }
    }

    /** The side a stored word names, {@code null} for nothing and for a word that names no side of a machine. */
    private static String stored(String word) {
        if (word == null || word.isEmpty() || MachineSides.NONE.equals(word)) {
            return null;
        }
        for (String known : WALK) {
            if (known.equals(word)) {
                return known;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("FaceConfig(front ").append(facing);
        text.append(", power in ").append(word(energyIn)).append(", power out ").append(word(energyOut))
                .append(", exhaust ").append(word(exhaust));
        for (int index = 0; index < tanks.length; index++) {
            text.append(", ").append(tankRoles[index]).append(' ').append(index).append(' ')
                    .append(word(tanks[index]));
        }
        return text.append(')').toString();
    }
}
