package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidNode;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.fluid.SimpleFluidStorage;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.pipe.PipeFlow;
import com.philia093.neofactory.pipe.PipeMaterial;
import com.philia093.neofactory.pipe.PipeTransport;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.pipe.Pipes.Pipe;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceMark;
import com.philia093.neofactory.world.interaction.FaceOperable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The block entity of a pipe: it is what a wrench is put to.
 * <p>
 * A pipe decides one thing - which of its six sides it joins - and that decision is the state of its cell,
 * see {@link Pipes}: the six properties of a pipe block are the six directions and the number of its state
 * is the mask of its connections. <b>Nothing of that happens by itself.</b> A pipe that is placed joins
 * nothing at all and stays that way: a line is joined where a player joined it and nowhere else.
 * <p>
 * <b>Why the wrench and not the neighbours.</b> A pipe that looked at the six cells around it would join
 * whatever it found there, and a player who builds two lines through one wall, a line beside a line, or a
 * line along a machine would end up with the tubes they did not ask for - and, once fluid runs in them,
 * with the fluid they did not ask for either. The world is therefore not read at all: the side of a pipe
 * carries the bit the player gave it, and a pipe whose neighbour was taken away keeps the ending it had,
 * which shows as an open tube with the plate of its size on it, see {@code tools/gen_pipe_models.ps1}.
 * <p>
 * <b>What a cell of the grid does.</b> A player who holds the wrench and looks at a pipe sees the grid of
 * nine cells of {@link FaceOperable}: every cell stands for one side of the pipe and a click turns that
 * side - open where it is closed, closed where it is open. Any side may be turned, whatever it faces, and
 * a side that faces a wall may be opened as well, because the wall may be gone a moment later. A click with
 * the modifier key held turns the <i>valve</i> of the side instead and leaves the join alone: both ways, in
 * only, out only, and both ways again, see {@link PipeFlow}.
 * <p>
 * <b>The tube holds a little fluid.</b> A pipe is no pump: every tick it offers what it holds to the pipes
 * it is joined to, in the shares their rates call for, and each of them takes what fits. How large the
 * storage of a pipe is, how much one tick of it moves and how a junction divides what it has is the
 * arithmetic of {@link PipeTransport}; what stands here is the fluid itself and the valve of every side.
 * <p>
 * <b>A pipe that is given a fluid it cannot take bursts.</b> The material of the pipe names the heat it
 * carries, see {@link PipeMaterial#maxTemperature()}, and a fluid above it destroys the pipe: it is
 * replaced by air, the fluid inside it is lost with it and the item is not dropped, because a tube that
 * gave way is a tube a player has to build again.
 * <p>
 * <b>The valve is the only thing this entity stores.</b> The fluid of a tube is not written to the save
 * file: a pipe carries what a machine next to it gives it, and a pipe that is unloaded or broken loses the
 * fluid that stood in it, which is what keeps a chunk from storing the whole of a line. The connections of
 * a pipe are its state and not data of the entity, see {@link Pipes}.
 */
public class PipeBlockEntity extends BlockEntity implements FaceOperable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Key the valve of the six sides is stored under. */
    private static final String FLOW_KEY = "flow";

    /** Bits one side of the pipe takes in the stored valve. */
    private static final int FLOW_BITS = 2;

    /** Mask of the bits of one side. */
    private static final int FLOW_MASK = (1 << FLOW_BITS) - 1;

    /** The valve of a side, in the order of their numbers, read by {@link #flowOf(BlockFace)}. */
    private static final PipeFlow[] FLOWS = PipeFlow.values();

    /**
     * The small storage of this tube.
     * <p>
     * Created the first time the pipe is ticked, because how much a pipe holds is what it moves a second
     * and that is a question of the block that stands in its cell, see {@link Pipes#of(Block)}.
     */
    private SimpleFluidStorage tube;

    /**
     * Millibuckets the tube still owes the line, the part of a tick that was left over.
     * <p>
     * A rate is written in millibuckets a second and a tick is a twentieth of one, so a pipe that moves
     * sixty a second owes three millibuckets a tick; the count is kept between ticks so that no rate is
     * lost to rounding, and it never grows past one buffer of the pipe, because a full pipe that cannot
     * pass the fluid on has no more to offer.
     */
    private float owed;

    /** Valve of the six sides, two bits each, in the order of {@link Pipes#DIRECTIONS}. */
    private int flow;

    /** Faces and pipes of the sides a tick gives to, written before they are used. */
    private final BlockFace[] givingFaces = new BlockFace[Pipes.DIRECTIONS.size()];

    /** Neighbours a tick gives to, in the same order as {@link #givingFaces}. */
    private final PipeBlockEntity[] givingPipes = new PipeBlockEntity[Pipes.DIRECTIONS.size()];

    /** Machines a tick pours into, in the same order as {@link #givingFaces}. */
    private final FluidNode[] givingNodes = new FluidNode[Pipes.DIRECTIONS.size()];

    /** Weight of every side a tick gives to, in the same order. */
    private final int[] givingWeights = new int[Pipes.DIRECTIONS.size()];

    /** Share of the offer every side a tick gives to takes, in the same order. */
    private final int[] givingShares = new int[Pipes.DIRECTIONS.size()];

    /**
     * Creates the block entity of a pipe.
     *
     * @param type type of this block entity
     */
    public PipeBlockEntity(BlockEntityType type) {
        super(type);
    }

    /**
     * Passes the fluid of this tube on, one tick at a time.
     * <p>
     * What the tube holds is offered to the sides it is joined to, each of them takes the share its rate
     * calls for and what is left stays where it is - a pipe holds what it cannot pass on instead of losing
     * it. A side that is set to take only is left out, a side that faces air or a wall is left out, and a
     * fluid hotter than the material of the pipe bursts the pipe instead of moving anywhere, see
     * {@link PipeTransport}. A machine behind a side is poured into as long as that side is its mouth, see
     * {@link FluidNode}.
     */
    @Override
    protected void update(World world, float delta) {
        Pipe pipe = Pipes.of(world.peekBlock(x(), y(), z()));
        if (pipe == null) {
            // The pipe of this cell is gone - it was taken away or it burst - so the tube holds nothing.
            tube = null;
            owed = 0.0f;
            return;
        }
        SimpleFluidStorage storage = tube(pipe);
        if (storage.isEmpty()) {
            owed = 0.0f;
            return;
        }
        if (PipeTransport.bursts(pipe, storage.fluid())) {
            burst(world, pipe, storage);
            return;
        }
        // The offer of a pipe grows with the time that passed and never past one buffer of it: a full pipe
        // has nothing more to offer than the fluid it holds.
        owed = Math.min(pipe.flow(), owed + PipeTransport.inTime(pipe.flow(), delta));
        int offer = Math.min(storage.amount(), (int) owed);
        if (offer <= 0) {
            return;
        }
        int sides = sidesThatGive(world, Pipes.maskOf(world.getState(x(), y(), z())));
        if (sides == 0) {
            return;
        }
        PipeTransport.split(offer, givingWeights, givingShares);
        int given = 0;
        for (int index = 0; index < sides; index++) {
            BlockFace side = givingFaces[index].opposite();
            if (givingPipes[index] != null) {
                given += givingPipes[index].give(side, storage.fluid(), givingShares[index]);
                continue;
            }
            // A machine: the share is poured into the tank its mouth reaches.
            FluidStorage tank = givingNodes[index].tankOn(side);
            if (tank != null) {
                given += tank.fill(storage.fluid(), givingShares[index], false);
            }
        }
        storage.drain(given, false);
        owed -= given;
    }

    /**
     * Collects the sides this pipe gives fluid to.
     * <p>
     * A side counts when it is joined, when its valve lets fluid out and when something that takes the fluid
     * stands there: a pipe behind the side takes what fits and is left alone while it stands under as much
     * pressure as this one, see {@link PipeTransport#flowsTo}, and a machine behind it is poured into, as
     * long as the side towards this pipe is the mouth of that machine, see {@link FluidNode}. A side that
     * faces air or a wall gives nothing.
     *
     * @param world world this pipe lies in
     * @param mask connections of this pipe
     * @return amount of sides that were collected
     */
    private int sidesThatGive(World world, int mask) {
        int sides = 0;
        for (BlockFace face : Pipes.DIRECTIONS) {
            givingFaces[sides] = face;
            givingPipes[sides] = null;
            givingNodes[sides] = null;
            givingWeights[sides] = 0;
            givingShares[sides] = 0;
            if (!Pipes.isConnected(mask, face) || !flowOf(face).gives()) {
                continue;
            }
            int otherX = x() + face.x();
            int otherY = y() + face.y();
            int otherZ = z() + face.z();
            Block otherBlock = world.peekBlock(otherX, otherY, otherZ);
            Pipe neighbour = Pipes.of(otherBlock);
            // A cell whose chunk is loaded reports its block here; the entity of one that carries a block
            // entity is asked for with the block, so a pipe that lost its entity is built again instead of
            // standing there as a cell nothing can reach, see World#ensureBlockEntity.
            BlockEntity entity = otherBlock.hasBlockEntity()
                    ? world.ensureBlockEntity(otherX, otherY, otherZ)
                    : null;
            if (neighbour != null && entity instanceof PipeBlockEntity other
                    && Pipes.isConnected(Pipes.maskOf(world.getState(otherX, otherY, otherZ)),
                            face.opposite())
                    && other.flowOf(face.opposite()).takes()
                    && PipeTransport.flowsTo(fluidAmount(), tubeCapacity(), other.fluidAmount(),
                            other.tubeCapacity())) {
                givingFaces[sides] = face;
                givingPipes[sides] = other;
                givingWeights[sides] = PipeTransport.weightOf(neighbour);
                sides++;
                continue;
            }
            if (entity instanceof FluidNode node && node.takesOn(face.opposite())) {
                // A machine behind this side: its mouth is a tank this pipe pours into, and a machine weighs
                // one, because it names no rate of its own - the machine is the pump of the line.
                givingFaces[sides] = face;
                givingNodes[sides] = node;
                givingWeights[sides] = PipeTransport.weightOf(null);
                sides++;
            }
        }
        return sides;
    }

    /**
     * The tube of this pipe, created the first time it is asked for.
     *
     * @param pipe the pipe this cell carries
     * @return the storage of the tube, as large as the pipe moves a second
     */
    private SimpleFluidStorage tube(Pipe pipe) {
        if (tube == null || tube.capacity() != pipe.flow()) {
            SimpleFluidStorage created = new SimpleFluidStorage(pipe.flow());
            if (tube != null) {
                // The pipe of this cell changed, which a wrench cannot do: the little fluid it held is
                // carried over instead of being lost.
                created.set(tube.fluid(), tube.amount());
            }
            tube = created;
        }
        return tube;
    }

    /**
     * Destroys a pipe that was given a fluid it cannot take.
     * <p>
     * The pipe is replaced by air, the fluid of the tube is lost with it and no item is dropped, because a
     * tube that gave way is a tube that has to be built again. The cell is left as a change of the world,
     * so a burst pipe is not put back by a chunk that is loaded from a file, see {@code World#setBlock}.
     *
     * @param world world this pipe lies in
     * @param pipe the pipe that gives way
     * @param storage the fluid it held
     */
    private void burst(World world, Pipe pipe, SimpleFluidStorage storage) {
        LOGGER.info("The {} pipe at ({}, {}, {}) burst: {} of {} at {} K is more than the {} K it takes",
                pipe.material().name(), x(), y(), z(), storage.amount(), storage.fluid().name(),
                storage.fluid().temperature(), pipe.maxTemperature());
        storage.set(null, 0);
        tube = null;
        owed = 0.0f;
        world.setBlock(x(), y(), z(), Blocks.AIR);
        world.setState(x(), y(), z(), 0);
    }

    /**
     * {@code true}: every pipe of the game shows the grid of faces.
     * <p>
     * A pipe has no state in which its sides cannot be turned - a line that is not built yet is worked on
     * as readily as one that runs - so the grid appears as soon as a player who holds the wrench looks at
     * one, see {@link FaceTool}.
     */
    @Override
    public boolean showsFaceGrid(World world, int x, int y, int z) {
        return true;
    }

    /**
     * The mark of one side of this pipe: crossed out where nothing is joined, an arrow where the valve
     * points one way, plain where the line runs both ways.
     *
     * @param world world the pipe lies in
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face side of the pipe the cell stands for
     * @return the mark of that side
     */
    @Override
    public FaceMark faceMark(World world, int x, int y, int z, BlockFace face) {
        if (!Pipes.isConnected(Pipes.maskOf(world.getState(x, y, z)), face)) {
            // A side that is not joined is the open end of a tube, and the grid crosses it out: a player
            // sees at a glance which of the nine sides the line really runs through, see FaceOverlay.
            return FaceMark.CLOSED;
        }
        switch (flowOf(face)) {
            case IN:
                return FaceMark.IN;
            case OUT:
                return FaceMark.OUT;
            default:
                return FaceMark.NOTHING;
        }
    }

    /**
     * Turns one side of this pipe with the wrench.
     * <p>
     * The side is closed when it is open and opened when it is closed, whatever stands next to the pipe:
     * <b>any side may be turned.</b> A player builds what they mean to build and a wrench is what says so -
     * a side that is opened towards nothing is the open ending of the tube, which the models draw with the
     * plate of the size on it, and a side that faces a wall may be opened as well, because the wall may be
     * gone a moment later. The rule of {@link Pipes#connects(Block)} is the question of the transport - may
     * a pipe take fluid from what stands there - and not a question a wrench asks.
     * <p>
     * <b>The modifier key turns the valve instead of the join.</b> A click with it held walks the side one
     * step along the cycle of {@link PipeFlow} - both ways, in only, out only - and leaves the join as it
     * was, so a player may set a mouth or an outlet without having to build the side again afterwards.
     * <p>
     * The state of the cell is written the way the generator of the models names it, so the cell redraws
     * itself with the tube of that side on it, see {@link Pipes#toggled(int, BlockFace)}.
     */
    @Override
    public boolean operateFace(World world, int x, int y, int z, BlockFace face, FaceTool tool,
            Player player, ItemStack held, boolean modifier) {
        if (tool != FaceTool.WRENCH || Pipes.of(world.peekBlock(x, y, z)) == null) {
            return false;
        }
        if (modifier) {
            PipeFlow valve = cycleFlow(face);
            LOGGER.info("The pipe at ({}, {}, {}) lets fluid through its {} side {}", x, y, z, face,
                    valve);
            return true;
        }
        world.setState(x, y, z, Pipes.toggled(world.getState(x, y, z), face));
        return true;
    }

    /**
     * The small storage inside this tube.
     * <p>
     * This is what a machine at the end of a line will fill and empty, see {@code FluidNode}, and what a
     * test fills to watch a line run. The pipe is named by the block of the cell and not by this entity, so
     * a cell that no longer carries a pipe answers {@code null}.
     *
     * @param world world this pipe lies in
     * @return the storage of the tube, or {@code null} when the cell holds no pipe
     */
    public FluidStorage storage(World world) {
        Pipe pipe = Pipes.of(world.peekBlock(x(), y(), z()));
        return pipe == null ? null : tube(pipe);
    }

    /**
     * The valve of one side of this pipe.
     *
     * @param side side of the pipe
     * @return the setting of that side, {@link PipeFlow#BOTH} while nobody turned it
     */
    public PipeFlow flowOf(BlockFace side) {
        return FLOWS[Math.min(FLOWS.length - 1, (flow >>> (indexOf(side) * FLOW_BITS)) & FLOW_MASK)];
    }

    /**
     * Sets the valve of one side of this pipe.
     *
     * @param side side of the pipe
     * @param valve setting to store
     */
    public void setFlow(BlockFace side, PipeFlow valve) {
        int shift = indexOf(side) * FLOW_BITS;
        flow = (flow & ~(FLOW_MASK << shift)) | (valve.ordinal() << shift);
    }

    /**
     * Turns the valve of one side one step further.
     *
     * @param side side of the pipe
     * @return the setting the side was given
     */
    public PipeFlow cycleFlow(BlockFace side) {
        PipeFlow next = flowOf(side).next();
        setFlow(side, next);
        return next;
    }

    /**
     * Fluid in the tube of this pipe.
     *
     * @return the amount in millibuckets, {@code 0} while the pipe was never ticked or holds nothing
     */
    public int fluidAmount() {
        return tube == null ? 0 : tube.amount();
    }

    /**
     * What the tube of this pipe holds when it is full.
     *
     * @return the capacity in millibuckets, {@code 0} while the pipe was never ticked
     */
    public int tubeCapacity() {
        return tube == null ? 0 : tube.capacity();
    }

    /**
     * Takes fluid through one side of this pipe.
     * <p>
     * Called by the pipe that stands behind that side, which offers what it has, see
     * {@code PipeTransport#split(int, int[], int[])}. A side that only gives takes nothing and an empty
     * pipe takes whatever fits; a pipe that was never ticked has no tube yet and refuses for that one tick,
     * see {@link #tube(Pipe)}.
     *
     * @param side side of this pipe the fluid arrives through
     * @param fluid kind of the offered fluid
     * @param amount largest amount that is offered
     * @return the amount that was taken
     */
    public int give(BlockFace side, Fluid fluid, int amount) {
        if (!flowOf(side).takes() || tube == null) {
            return 0;
        }
        return tube.fill(fluid, amount, false);
    }

    /** Position of a side of a pipe in the order of {@link Pipes#DIRECTIONS}. */
    private static int indexOf(BlockFace side) {
        int index = Pipes.DIRECTIONS.indexOf(side);
        if (index < 0) {
            throw new IllegalArgumentException("A pipe has no side towards " + side);
        }
        return index;
    }

    @Override
    protected void writeOwnData(NbtCompound data) {
        // The valve of the six sides, and nothing else: the connections of a pipe are the state of its cell
        // and the fluid in its tube is not written, see the class comment.
        data.putInt(FLOW_KEY, flow);
    }

    @Override
    protected void readOwnData(NbtCompound data) {
        flow = data.getInt(FLOW_KEY, 0);
    }

    @Override
    public String toString() {
        return "PipeBlockEntity(" + type().name() + "@" + x() + "," + y() + "," + z() + ")";
    }
}
