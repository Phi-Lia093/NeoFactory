package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.block.state.BlockStateTable;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidNode;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.MachineTank;
import com.philia093.neofactory.machine.SteamMachine;
import com.philia093.neofactory.pipe.PipeTransport;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceOperable;
import com.philia093.neofactory.world.save.SaveTags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The block entity of a machine: what a machine looks like from the world.
 * <p>
 * The class is the bridge between the two halves of a machine. The world only knows
 * block entities - it ticks them and stores them with their chunk - while a machine
 * knows nothing about the world at all and can therefore be tested without one. This
 * class hands the tick on, drops the state into the save game and hands the content of
 * the machine to the world while its block is broken.
 * <p>
 * A machine that needs to look at its neighbours overrides {@code update} in a subclass
 * and keeps the world it was given.
 */
public class MachineBlockEntity extends BlockEntity implements FluidNode, FaceOperable {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Direction a machine looks in before anybody turns it.
     * <p>
     * The mouth is the face fluid runs <i>into</i> the machine through - the art of a machine shows it as
     * the opening of its front - and every other side of a machine that makes a fluid is a hatch that gives
     * what the machine made, see {@link FluidNode}. A player turns both the machine and the face its steam
     * blows out of with the wrench: the right button sets the direction, the right button with the modifier
     * key sets the exhaust, see {@link #operateFace}.
     */
    public static final BlockFace MOUTH = BlockFace.NORTH;

    /**
     * Property a block of a machine uses to say that the machine works.
     * <p>
     * A block that carries it draws another picture while the machine runs - the mouth of a boiler glows
     * when it burns - which is what the state of a cell is for, see {@link BlockStateTable}. A block
     * without it is simply never relit.
     */
    public static final String LIT = "lit";

    private final Machine machine;

    /** Side the machine looks in, turned by the wrench and stored with the machine. */
    private BlockFace facing = MOUTH;

    /** Side the steam of a steam machine blows out of, set with the wrench and the modifier key. */
    private BlockFace exhaustFace = MOUTH.opposite();

    /**
     * Creates the block entity of a machine.
     *
     * @param type type of this block entity
     * @param machine machine behind this block
     */
    public MachineBlockEntity(BlockEntityType type, Machine machine) {
        super(type);
        this.machine = Objects.requireNonNull(machine, "machine");
    }

    /** Machine behind this block. */
    public Machine machine() {
        return machine;
    }

    @Override
    protected void update(World world, float delta) {
        machine.tick(delta);
        if (machine.isExploded()) {
            ruin(world);
            return;
        }
        updateExhaust(world);
        pourIntoPipes(world);
        updateLitState(world);
    }

    /**
     * Looks at the exhaust of a steam machine, the face its spent steam blows out of.
     * <p>
     * <b>A steam machine has to breathe.</b> The moment a craft that spends steam ends, the block looks at
     * the face the exhaust was set to, and a solid block standing there is reported and the machine refuses
     * the next recipe until the way is open again - the craft that was already running is finished, because
     * its steam was paid for and used, see {@link SteamMachine}. A machine that is waiting is looked at every
     * tick as well, so clearing the wall lets it start again by itself.
     *
     * @param world world this machine lies in
     */
    private void updateExhaust(World world) {
        if (!(machine instanceof SteamMachine steam)) {
            return;
        }
        if (!steam.takesAnExhaustCheck() && !steam.isWaitingForExhaust()) {
            return;
        }
        BlockFace face = exhaustFace;
        // A cell of a chunk that is not loaded reports air, so a machine at the edge of the built world is
        // never blocked by terrain nobody has raised yet.
        boolean blocked = world.peekBlock(x() + face.x(), y() + face.y(), z() + face.z()).isSolid();
        if (blocked != steam.isWaitingForExhaust()) {
            if (blocked) {
                LOGGER.warn("The {} at ({}, {}, {}) cannot blow its steam out of its {} side, a solid block "
                        + "stands there, so it refuses the next recipe", machine.name(), x(), y(), z(), face);
            } else {
                LOGGER.info("The {} at ({}, {}, {}) can blow its steam out of its {} side again",
                        machine.name(), x(), y(), z(), face);
            }
        }
        steam.reportExhaust(blocked);
    }

    /**
     * {@code true}: every machine shows the grid of faces.
     * <p>
     * A player who holds the wrench at a machine sets the side it looks in and the side its steam blows out
     * of, see {@link #operateFace}.
     */
    @Override
    public boolean showsFaceGrid(World world, int x, int y, int z) {
        return true;
    }

    /**
     * Turns the machine, or sets where its steam blows out.
     * <p>
     * <b>The right button turns the machine</b>: the side a player clicked is the side the machine looks in
     * from then on, which is the side its mouth - the tank fluid runs into - is reached through. <b>The right
     * button with the modifier key sets the exhaust</b>: the steam of a recipe leaves through that face, and
     * a solid block standing there is reported when a craft ends, see {@code SteamMachine}.
     *
     * @param world world the machine lies in
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face side of the machine that was clicked
     * @param tool tool the operation is carried out with
     * @param player player who works on the machine
     * @param held stack the player holds
     * @param modifier {@code true} to set the exhaust instead of the direction
     * @return {@code true} when something was set
     */
    @Override
    public boolean operateFace(World world, int x, int y, int z, BlockFace face, FaceTool tool,
            Player player, ItemStack held, boolean modifier) {
        if (tool != FaceTool.WRENCH) {
            return false;
        }
        if (modifier) {
            exhaustFace = face;
            LOGGER.info("The {} at ({}, {}, {}) blows its steam out of its {} side", machine.name(), x, y, z,
                    face);
            return true;
        }
        facing = face;
        LOGGER.info("The {} at ({}, {}, {}) now looks towards its {} side", machine.name(), x, y, z, face);
        return true;
    }

    /** Side the machine looks in, the face its mouth is reached through. */
    public BlockFace facing() {
        return facing;
    }

    /** Side the steam of this machine blows out of. */
    public BlockFace exhaustFace() {
        return exhaustFace;
    }

    @Override
    public FluidStorage tankOn(BlockFace face) {
        // The mouth of a machine is the face fluid runs into and its other five sides give what the machine
        // made, see FluidNode. A machine that makes no fluid - a steam machine, which blows its steam out of
        // the exhaust and keeps nothing - is filled from every side instead, because there is no hatch of it
        // to tell the mouth from.
        if (tankOf(MachineTank.Role.OUTPUT) == null) {
            return tankOf(MachineTank.Role.INPUT);
        }
        return face == facing ? tankOf(MachineTank.Role.INPUT) : tankOf(MachineTank.Role.OUTPUT);
    }

    @Override
    public boolean takesOn(BlockFace face) {
        return (tankOf(MachineTank.Role.OUTPUT) == null || face == facing) && tankOn(face) != null;
    }

    /** The first tank of a role, {@code null} when the machine holds none of them. */
    private FluidStorage tankOf(MachineTank.Role role) {
        List<MachineTank> tanks = machine.tanksOf(role);
        return tanks.isEmpty() ? null : tanks.get(0).storage();
    }

    /**
     * Pours what the machine made into the lines that stand next to it.
     * <p>
     * A machine is a pump and a pipe is not, so the machine is the one that pushes: every tick it offers its
     * output tank to the machines and pipes around it and every pipe takes what it can pass on in one tick -
     * so a wide line is filled faster than a narrow one and all of them together empty the tank. The mouth is
     * left out, because that is the face the machine takes fluid in through and never gives it out of. A
     * pipe that has its own valve shut on the side towards the machine takes nothing, which is what makes a
     * player able to cut a machine off with the wrench, see {@code PipeFlow}.
     *
     * @param world world this machine lies in
     */
    private void pourIntoPipes(World world) {
        FluidStorage tank = tankOf(MachineTank.Role.OUTPUT);
        if (tank == null || tank.isEmpty()) {
            return;
        }
        Fluid fluid = tank.fluid();
        for (BlockFace face : BlockFace.ALL) {
            if (face == MOUTH) {
                continue;
            }
            int otherX = x() + face.x();
            int otherY = y() + face.y();
            int otherZ = z() + face.z();
            Block otherBlock = world.peekBlock(otherX, otherY, otherZ);
            Pipes.Pipe pipe = Pipes.of(otherBlock);
            if (pipe == null) {
                continue;
            }
            BlockEntity entity = world.ensureBlockEntity(otherX, otherY, otherZ);
            if (!(entity instanceof PipeBlockEntity line)) {
                continue;
            }
            if (!Pipes.isConnected(Pipes.maskOf(world.getState(otherX, otherY, otherZ)), face.opposite())) {
                // The pipe has that side closed, so the machine pours nothing into it: a line is joined where
                // a player joined it and nowhere else, see Pipes.
                continue;
            }
            int offer = Math.min(tank.amount(), PipeTransport.perTick(pipe.flow()));
            int moved = line.give(face.opposite(), fluid, offer);
            tank.drain(moved, false);
            if (tank.isEmpty()) {
                return;
            }
        }
    }

    /**
     * Takes the machine out of the world after it ruined itself.
     * <p>
     * The block goes and the entity with it, and everything the machine held goes as well: the cell is
     * replaced without a loot table being asked and without {@link #onBroken} being called, so a boiler that
     * exploded leaves nothing behind but the hole it stood in - neither its slots nor its tanks are handed to
     * the player. Only the block itself is destroyed; the world around it is untouched, which is the part of
     * an explosion that is not written yet.
     */
    private void ruin(World world) {
        LOGGER.info("The {} at ({}, {}, {}) ruined itself", machine.name(), x(), y(), z());
        world.setBlock(x(), y(), z(), Blocks.AIR);
    }

    /**
     * Says in the state of the cell whether the machine works.
     * <p>
     * A machine that runs is drawn with the glowing picture of its front, see {@link #LIT}: the state
     * travels with the chunk like the block itself, so a world that is opened again shows a boiler that
     * still burns. The lookup only runs for a block that carries the property at all, so the cost is one
     * map lookup per tick for every machine of the game.
     */
    private void updateLitState(World world) {
        BlockStateTable states = world.getBlock(x(), y(), z()).states();
        if (!states.hasProperty(LIT)) {
            return;
        }
        int state = world.getState(x(), y(), z());
        Map<String, String> values = new LinkedHashMap<>(states.decode(state));
        String wanted = machine.isRunning() ? "true" : "false";
        if (wanted.equals(values.get(LIT))) {
            return;
        }
        values.put(LIT, wanted);
        world.setState(x(), y(), z(), states.stateOf(values));
    }

    @Override
    protected void writeOwnData(NbtCompound data) {
        machine.save(data);
        data.putString(SaveTags.MACHINE_FACING, facing.name());
        data.putString(SaveTags.MACHINE_EXHAUST, exhaustFace.name());
    }

    @Override
    protected void readOwnData(NbtCompound data) {
        machine.load(data);
        facing = faceByName(data.getString(SaveTags.MACHINE_FACING, MOUTH.name()), MOUTH);
        exhaustFace = faceByName(data.getString(SaveTags.MACHINE_EXHAUST, MOUTH.opposite().name()),
                MOUTH.opposite());
    }

    /** A side by its name, so a stored machine is never left without one. */
    private static BlockFace faceByName(String name, BlockFace fallback) {
        for (BlockFace face : BlockFace.ALL) {
            if (face.name().equals(name)) {
                return face;
            }
        }
        return fallback;
    }

    @Override
    public void onBroken(ItemDrops drops, float worldX, float worldZ) {
        machine.dumpItems(drops, worldX, worldZ);
    }

    @Override
    public String toString() {
        return "MachineBlockEntity(" + type().name() + " " + machine + ")";
    }
}
