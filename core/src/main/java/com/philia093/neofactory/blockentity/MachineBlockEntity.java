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
import com.philia093.neofactory.energy.EnergyGrid;
import com.philia093.neofactory.energy.EnergyNet;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.machine.EnergyStorage;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.ExhaustMachine;
import com.philia093.neofactory.machine.FaceConfig;
import com.philia093.neofactory.machine.MachineSides;
import com.philia093.neofactory.machine.MachineTank;
import com.philia093.neofactory.machine.SteamMachine;
import com.philia093.neofactory.pipe.PipeTransport;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceAppearance;
import com.philia093.neofactory.world.interaction.FaceClick;
import com.philia093.neofactory.world.interaction.FaceMark;
import com.philia093.neofactory.world.interaction.FaceOperable;
import com.philia093.neofactory.world.interaction.FacePicture;
import com.philia093.neofactory.world.interaction.FaceRole;
import com.philia093.neofactory.world.save.SaveTags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
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
public class MachineBlockEntity extends BlockEntity
        implements FluidNode, FaceOperable, FaceAppearance {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Direction a machine looks in before anybody turns it.
     * <p>
     * The front is the side the art of a machine shows and the one side of it that carries nothing: no pipe,
     * no cable and no belt is ever built against it, see {@link FaceConfig}. A machine is turned with the
     * wrench and keeps to the four sides of the horizon while it is, see {@link #operateFace}.
     * <p>
     * The side is the one {@link MachineSides#DEFAULT_FRONT} names, so the place the front of a machine
     * starts lies in one class only.
     */
    public static final BlockFace MOUTH = MachineSides.DEFAULT_FRONT;

    /**
     * Property a block of a machine uses to say which way its front looks.
     * <p>
     * A machine is turned while the game runs, so the side it looks in has to reach the mesh of its chunk:
     * the state of the cell is what the mesher reads, so the entity writes the side into it the moment the
     * wrench turns the machine, see {@link #updateFacingState(World)}.
     */
    public static final String FACING = "facing";

    /**
     * Property a block of a machine uses to say that the machine works.
     * <p>
     * A block that carries it draws another picture while the machine runs - the mouth of a boiler glows
     * when it burns - which is what the state of a cell is for, see {@link BlockStateTable}. A block
     * without it is simply never relit.
     */
    public static final String LIT = "lit";

    /**
     * Seconds a machine stays lit after the last frame of work it did, see {@link #updateLitState(World, float)}.
     * <p>
     * A quarter of a second covers the gap between two crafts of a machine that is fed without pause, and it is
     * short enough that a machine which really stopped goes dark while a player watches it.
     */
    private static final float LIT_HOLD_SECONDS = 0.25f;

    /** Seconds this machine has left to stay lit after the last frame of work it did. */
    private float litHold;

    private final Machine machine;

    /** Side the machine looks in, turned by the wrench and stored with the machine. */
    private BlockFace facing = MOUTH;

    /**
     * {@code true} once the side this machine looks in was read from the block and written back.
     * <p>
     * A block is placed turned towards the player who built it, which is a turn of the state of the cell and
     * no turn of the machine, so the first tick of a machine takes that side over and writes it back with
     * the jobs of the sides in mind, see {@link #settleFacing(World)}.
     */
    private boolean facingSettled;

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
        if (!facingSettled) {
            settleFacing(world);
        }
        machine.tick(delta);
        if (machine.isExploded()) {
            ruin(world);
            return;
        }
        updateExhaust(world);
        pourIntoPipes(world);
        updateEnergy(world);
        updateLitState(world, delta);
    }

    /**
     * Brings the power this machine wants out of the line of cables it stands on.
     * <p>
     * <b>A cable carries nothing of its own, so the machine that works is the one that moves the energy.</b>
     * A machine that makes power only fills its own buffer; the machine that wants it reaches for it: every
     * tick it asks for what it needs, {@link Machine#requestEu()}, and this class hands the question to the
     * side a player gave to the plug it takes power in through. The line is walked from that plug and what it
     * brings is drawn out of the buffers that may give, see {@link EnergyGrid.Line#pull}. A machine that asks
     * for nothing - a machine of the age of steam, a furnace that burns coal - never touches a line at all.
     * <p>
     * <b>A cable that is not joined towards the machine is not the line of that machine.</b> The walk of a
     * line only follows the sides a cable joins, so a cable that stands at the plug of a machine without
     * reaching it leaves the machine out of the ends of that line - and a machine asks a line that does not
     * reach it for nothing, which is what a player who cut the join with the wrench asked for, see
     * {@link EnergyGrid#line}.
     * <p>
     * <b>Two machines with no cable between them need no line.</b> When nothing but the neighbouring block
     * stands at the plug, the machine takes what the buffer beside it can give over the gap with no loss,
     * which is the way the first workshop of the age of electricity is built, see {@link EnergyNet#hand}.
     * <b>A gap is a line of the tier of the machine that gives</b>, so a machine of a later age destroys the
     * one beside it the way a line of that age does - and there is no cable of that line to be taken away with
     * it, see {@link EnergyNet#overvolts(EnergyStorage, EnergyStorage)}.
     * <p>
     * <b>A line that is too much for a machine takes it away.</b> A line of a higher tier than the machine
     * was built for does not feed it: the machine and every cable of the line are taken out of the world, see
     * {@code EnergyAcceptor}. The tier is settled by the machine that asks and never by a machine that only
     * makes power, so a wrong tier costs what the machine cost.
     *
     * @param world world this machine lies in
     */
    private void updateEnergy(World world) {
        EnergyStorage buffer = machine.energy();
        if (!buffer.canReceive() || buffer.isFull()) {
            // A machine that holds no buffer, or whose buffer is full, has no reason to reach for a line.
            return;
        }
        int wanted = Math.min(machine.requestEu(), buffer.capacity() - buffer.amount());
        if (wanted <= 0) {
            return;
        }
        BlockFace plug = machine.faces().energyIn();
        if (plug == null) {
            return;
        }
        int cellX = x() + plug.x();
        int cellY = y() + plug.y();
        int cellZ = z() + plug.z();
        EnergyGrid.Cells cells = EnergyGrid.of(world);
        EnergyGrid.Line line = EnergyGrid.line(cells, cellX, cellY, cellZ);
        if (line != null && line.reaches(buffer)) {
            line.pull(cells, buffer, wanted);
            return;
        }
        // Nothing but the two machines: the energy crosses the gap with no cable and no loss. What feeds this
        // machine is then the machine beside it, so a machine of a later age destroys it the way a line of
        // that age does - the energy is not handed over at all, and there is no cable of that line to be
        // taken away with it, see EnergyNet#overvolts.
        if (world.blockEntity(cellX, cellY, cellZ) instanceof MachineBlockEntity neighbour) {
            EnergyStorage source = neighbour.machine().energy();
            if (EnergyNet.overvolts(source, buffer)) {
                LOGGER.info("The {} at ({}, {}, {}) was taken by the {} beside it, which is built for a line "
                        + "of a later age", machine.name(), x(), y(), z(), neighbour.machine().name());
                world.setBlock(x(), y(), z(), Blocks.AIR);
                return;
            }
            EnergyNet.hand(source, buffer, wanted);
        }
    }

    /**
     * Looks at the exhaust of a steam machine, the face its spent steam blows out of.
     * <p>
     * <b>A steam machine has to breathe.</b> The moment a craft that spends steam ends, the block looks at
     * the face the exhaust was set to, and a solid block standing there is reported and the machine refuses
     * the next recipe until the way is open again - the craft that was already running is finished, because
     * its steam was paid for and used, see {@link SteamMachine} and {@link ExhaustMachine}. A machine that is
     * waiting is looked at every tick as well, so clearing the wall lets it start again by itself.
     *
     * @param world world this machine lies in
     */
    private void updateExhaust(World world) {
        if (!(machine instanceof ExhaustMachine breather)) {
            return;
        }
        if (!breather.takesAnExhaustCheck() && !breather.isWaitingForExhaust()) {
            return;
        }
        BlockFace face = machine.faces().exhaust();
        if (face == null) {
            // A machine that is no machine of steam has no vent at all, which is the shape its sides were
            // built with, see FaceConfig.
            return;
        }
        // A cell of a chunk that is not loaded reports air, so a machine at the edge of the built world is
        // never blocked by terrain nobody has raised yet.
        boolean blocked = world.peekBlock(x() + face.x(), y() + face.y(), z() + face.z()).isSolid();
        if (blocked != breather.isWaitingForExhaust()) {
            if (blocked) {
                LOGGER.warn("The {} at ({}, {}, {}) cannot blow its steam out of its {} side, a solid block "
                        + "stands there, so it refuses the next work", machine.name(), x(), y(), z(), face);
            } else {
                LOGGER.info("The {} at ({}, {}, {}) can blow its steam out of its {} side again",
                        machine.name(), x(), y(), z(), face);
            }
        }
        breather.reportExhaust(blocked);
    }

    /**
     * {@code true}: every machine shows the grid of faces.
     * <p>
     * A player who holds the wrench at a machine turns it, gives one of its sides a job, or takes the whole
     * machine apart with the left button. Which of those a click means is read from the button and from the
     * modifier key, see {@link FaceClick} and {@link #operateFace}.
     */
    @Override
    public boolean showsFaceGrid(World world, int x, int y, int z) {
        return true;
    }

    /**
     * Does what a click with the wrench on one side of a machine asks for.
     * <p>
     * <b>The right button turns the machine</b>: the side a player clicked is the side it looks in from then
     * on, and the side it turns onto gives up whatever job it had - a machine that is turned onto the side a
     * pipe was built against leaves the pipe behind with a mouth that reaches nothing, see
     * {@link FaceConfig#turned(BlockFace)}.
     * <p>
     * <b>The modifier key gives a side a job.</b> With the left button the side becomes the one that takes
     * something in - the vent a machine of steam blows out of, the plug the power of a machine comes in
     * through - and with the right button the one that gives something out, the plug a generator feeds the
     * line of the cables through. A side that already carries that very job loses it, so the same click
     * takes a job away again, and a side the machine has no use for is refused, see {@link FaceConfig}.
     * <p>
     * <b>The left button without the modifier key takes the machine apart</b> and never reaches here: it is
     * the click the world spends on mining, see {@code GameScreen#updateInteraction}.
     *
     * @param world world the machine lies in
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face side of the machine that was clicked
     * @param tool tool the operation is carried out with
     * @param player player who works on the machine
     * @param held stack the player holds
     * @param click which button was pressed and whether the modifier key was held
     * @return {@code true} when the machine or one of its sides changed
     */
    @Override
    public boolean operateFace(World world, int x, int y, int z, BlockFace face, FaceTool tool,
            Player player, ItemStack held, FaceClick click) {
        if (tool != FaceTool.WRENCH || click == FaceClick.LEFT) {
            return false;
        }
        boolean done = switch (click) {
            case RIGHT -> turn(world, face);
            case SHIFT_LEFT -> give(face, true);
            default -> give(face, false);
        };
        if (done) {
            // The sides of a machine are what the block entity draws, and the state of the cell says nothing
            // about them: the section is asked to be meshed again, see World#markDirty.
            world.markDirty(x, y, z);
        }
        return done;
    }

    /**
     * Turns this machine onto one of its sides.
     *
     * @param world world the machine lies in
     * @param face side the machine looks in from now on
     * @return {@code true} when the machine was turned
     */
    private boolean turn(World world, BlockFace face) {
        if (!machine.faces().hasFront()) {
            // A box of cells is the same from every side of it: there is no front to turn towards, so the
            // wrench leaves such a machine as it is, see FaceConfig#withoutFront.
            return false;
        }
        if (face == facing || !MachineSides.isHorizontal(face)) {
            // A machine looks along the horizon, because the four names a player reads for the sides of its
            // front - left and right, up and down - only mean something then, see MachineSides.
            return false;
        }
        facing = face;
        facingSettled = true;
        // The words a player reads at the sides of this machine are read from its front, so the machine keeps
        // every side it was given whatever way it is turned, see FaceConfig#facing.
        machine.faces().facing(face);
        updateFacingState(world);
        LOGGER.info("The {} at ({}, {}, {}) now looks towards its {} side", machine.name(), x(), y(), z(),
                face);
        return true;
    }

    /**
     * Gives one side of this machine a job, or takes the job it carries away again.
     *
     * @param face side the job is put on
     * @param incoming {@code true} for the job of taking something in, {@code false} for giving it out
     * @return {@code true} when a side of the machine changed
     */
    private boolean give(BlockFace face, boolean incoming) {
        if (machine.faces().hasFront() && face == facing) {
            // The front carries nothing, so no job is ever put on it, see FaceConfig.
            return false;
        }
        FaceConfig faces = machine.faces();
        if (incoming) {
            if (faces.blowsSteam()) {
                return faces.setExhaust(away(faces.exhaust(), face));
            }
            return faces.setEnergyIn(away(faces.energyIn(), face));
        }
        return faces.setEnergyOut(away(faces.energyOut(), face));
    }

    /** The side a job moves to, {@code null} when the click lands on the side that already carries it. */
    private static BlockFace away(BlockFace owner, BlockFace face) {
        return owner == face ? null : face;
    }

    /**
     * Side the machine looks in, the one side of it that carries nothing.
     * <p>
     * <b>A machine without a front looks nowhere</b>, see {@link FaceConfig#hasFront()}: this answers with the
     * side such a machine is built towards - the north - and nothing of the machine is read from it, because
     * no side of it carries nothing and none of them is ever crossed out, see {@link #faceMark}.
     */
    public BlockFace facing() {
        return facing;
    }

    /** Side this machine blows its spent steam out of, {@code null} when it is no machine of steam. */
    public BlockFace exhaustFace() {
        return machine.faces().exhaust();
    }

    /**
     * Reads the side the block was built with, and writes the side the machine looks in back into the cell.
     * <p>
     * A machine is placed turned towards the player who built it: that turn is a turn of the state of the
     * cell, written before the entity of the machine exists, so the side belongs to the machine itself only
     * from its first tick on. The turn is taken over here - the jobs of the sides are read with it - and the
     * side is written back, which is a write that changes nothing at all when the two already agree.
     *
     * @param world world the machine lies in
     */
    private void settleFacing(World world) {
        facingSettled = true;
        BlockStateTable states = world.getBlock(x(), y(), z()).states();
        if (!states.hasProperty(FACING)) {
            return;
        }
        BlockFace placed = BlockFace.byName(states.decode(world.getState(x(), y(), z())).get(FACING));
        if (placed != null && MachineSides.isHorizontal(placed) && placed != facing) {
            facing = placed;
            // The sides of a machine are read from its front, so the machine that was placed keeps the sides it
            // was built with and shows them on the flanks of the front it was placed with, see FaceConfig.
            machine.faces().facing(placed);
        }
        updateFacingState(world);
    }

    /**
     * Writes the side this machine looks in into the state of its cell.
     * <p>
     * The turn of a state is what carries the front of a model around the vertical axis, so a machine that is
     * turned keeps the picture of its model until the state of its cell says otherwise: the side the wrench
     * set is written the moment it is set, and the mesher of the section draws the front of the machine on
     * the side a player turned it to. A block that carries no such property is left alone, which keeps one
     * machine of the game from depending on the model of another.
     *
     * @param world world the machine lies in
     */
    private void updateFacingState(World world) {
        BlockStateTable states = world.getBlock(x(), y(), z()).states();
        if (!states.hasProperty(FACING)) {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>(states.decode(world.getState(x(), y(), z())));
        String wanted = facing.toString();
        if (wanted.equals(values.get(FACING))) {
            return;
        }
        values.put(FACING, wanted);
        world.setState(x(), y(), z(), states.stateOf(values));
    }

    /**
     * The pictures one side of this machine is drawn with.
     * <p>
     * A side a player gave a job to shows the casing of the machine with the overlay of that job over it -
     * the stub of a pipe for a fluid, the plug of the power for the line of the cables - and every other
     * side, the front included, keeps the picture the model of its state carries there.
     *
     * @param face side of the block, as the world has it
     * @return the two pictures of that side, {@code null} when the model of the machine stands
     */
    @Override
    public FacePicture pictureOn(BlockFace face) {
        FaceRole role = machine.faces().roleOn(face);
        return role == FaceRole.NONE ? null : FacePicture.of(machine.casing(), role);
    }

    /**
     * What the grid of faces says about one side of this machine.
     * <p>
     * The side a machine looks in is crossed out, because it is the one side of a machine that carries
     * nothing at all: no pipe and no cable is ever built against the front. A side that takes something in
     * carries the arrow that points into the block and a side that gives something out the arrow that points
     * away, whatever the job is - a player reads from the arrow whether a side is a mouth or a plug, see
     * {@link FaceMark}.
     *
     * @param face side of the block the cell stands for
     * @return the mark of that side
     */
    @Override
    public FaceMark faceMark(World world, int x, int y, int z, BlockFace face) {
        if (machine.faces().hasFront() && face == facing) {
            // The grid crosses out the front of a machine, see FaceConfig. A machine without a front has no
            // side that carries nothing, so every side of it carries a mark.
            return FaceMark.CLOSED;
        }
        return switch (machine.faces().roleOn(face)) {
            case FLUID_IN -> FaceMark.IN;
            case FLUID_OUT -> FaceMark.OUT;
            case ENERGY_IN -> FaceMark.ENERGY_IN;
            case ENERGY_OUT -> FaceMark.ENERGY_OUT;
            case EXHAUST -> FaceMark.EXHAUST;
            default -> FaceMark.NOTHING;
        };
    }

    /**
     * The tank one side of this machine is reached through.
     *
     * @param face side of the block
     * @return the tank on that side, {@code null} when no tank is reached from there
     */
    @Override
    public FluidStorage tankOn(BlockFace face) {
        int index = tankAt(face);
        return index < 0 ? null : machine.tank(index).storage();
    }

    /**
     * {@code true} when fluid may run into this machine through a side.
     * <p>
     * Only a side a player gave to a tank that is filled answers yes: the side of a tank that is emptied is
     * the side a machine gives what it made out of, and a side that carries no tank at all reaches nothing.
     * Which side that is is the configuration of the machine, see {@link FaceConfig}.
     */
    @Override
    public boolean takesOn(BlockFace face) {
        int index = tankAt(face);
        return index >= 0 && machine.tank(index).isInput();
    }

    /** Index of the tank a side is reached through, {@code -1} when no tank of the machine is. */
    private int tankAt(BlockFace face) {
        FaceConfig faces = machine.faces();
        for (int index = 0; index < faces.tankCount(); index++) {
            if (faces.faceOfTank(index) == face) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Pours what the machine made into the lines that stand next to it.
     * <p>
     * A machine is a pump and a pipe is not, so the machine is the one that pushes: every tick it offers
     * every tank that is emptied to the pipe that stands on the side of that tank and the pipe takes what it
     * can pass on in one tick - so a wide line is filled faster than a narrow one and the tanks are emptied.
     * <b>A tank that a player gave no side to is poured into nothing</b>, which is what the wheel of the
     * interface is for, and a tank that is filled never pours at all: its side is the mouth of the machine,
     * see {@link FluidNode}. A pipe that has its own valve shut on the side towards the machine takes
     * nothing, which is what makes a player able to cut a machine off with the wrench, see {@code PipeFlow}.
     *
     * @param world world this machine lies in
     */
    private void pourIntoPipes(World world) {
        FaceConfig faces = machine.faces();
        for (int index = 0; index < faces.tankCount(); index++) {
            if (faces.roleOfTank(index) != MachineTank.Role.OUTPUT) {
                continue;
            }
            BlockFace face = faces.faceOfTank(index);
            FluidStorage tank = machine.tank(index).storage();
            if (face == null || tank.isEmpty()) {
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
            Fluid fluid = tank.fluid();
            int offer = Math.min(tank.amount(), PipeTransport.perTick(pipe.flow()));
            int moved = line.give(face.opposite(), fluid, offer);
            tank.drain(moved, false);
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
     * <p>
     * <b>A machine that keeps working keeps glowing.</b> The state is not turned off the moment a craft ends
     * but stands for {@link #LIT_HOLD_SECONDS} after the last frame of work, because a machine that hands one
     * craft over and takes the next one a frame later is busy the whole time: turning the picture off in
     * between made the front of a working machine flicker like a lamp, and what a player has to see is whether
     * the machine works at all.
     *
     * @param world world this machine lies in
     * @param delta time since the last frame in seconds
     */
    private void updateLitState(World world, float delta) {
        if (machine.isRunning()) {
            litHold = LIT_HOLD_SECONDS;
        } else {
            litHold = Math.max(0.0f, litHold - delta);
        }
        BlockStateTable states = world.getBlock(x(), y(), z()).states();
        if (!states.hasProperty(LIT)) {
            return;
        }
        int state = world.getState(x(), y(), z());
        Map<String, String> values = new LinkedHashMap<>(states.decode(state));
        String wanted = litHold > 0.0f ? "true" : "false";
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
    }

    @Override
    protected void readOwnData(NbtCompound data) {
        machine.load(data);
        BlockFace stored = faceByName(data.getString(SaveTags.MACHINE_FACING, MOUTH.name()), MOUTH);
        // A machine looks along the horizon, see MachineSides: a world that was stored before that rule
        // existed keeps the front the machine is built with.
        facing = MachineSides.isHorizontal(stored) ? stored : MOUTH;
        // The sides of the machine are read from its front, so the front the machine was stored with is handed
        // to the assignment of its sides before anything else reads it, see FaceConfig#facing.
        machine.faces().facing(facing);
        facingSettled = false;
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
