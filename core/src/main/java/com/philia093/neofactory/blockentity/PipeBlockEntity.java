package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceOperable;

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
 * side - open where it is closed, closed where it is open. A side is only opened towards something that
 * may take fluid, so a click on a side that faces stone does nothing, see {@link Pipes#connects(Block)}.
 * <p>
 * <b>What the pipe will hold.</b> The fluid in the tube, what flows where and how hot it is, arrives with
 * the transport of fluids: a pipe will hold a small storage of its own and pass what it has to the pipe
 * next to it. Nothing of that is stored here yet, so the entity of a pipe writes no data of its own.
 */
public class PipeBlockEntity extends BlockEntity implements FaceOperable {

    /**
     * Creates the block entity of a pipe.
     *
     * @param type type of this block entity
     */
    public PipeBlockEntity(BlockEntityType type) {
        super(type);
    }

    /**
     * Does nothing: a pipe has no work of its own.
     * <p>
     * The sides of a pipe are turned by a player and not by time, so there is nothing here to advance - a
     * world that ticks a million times changes no mask. The transport of fluid will do its work in this
     * method one day: the tube that is filled, what moves on and how hot what is left still is. Until then
     * a pipe is a shape and the room it keeps for the fluid to come.
     */
    @Override
    protected void update(World world, float delta) {
        // Nothing of its own to do, see the comment above.
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
     * Turns one side of this pipe with the wrench.
     * <p>
     * The side is closed when it is open and opened when it is closed, whatever stands next to the pipe:
     * <b>any side may be turned.</b> A player builds what they mean to build and a wrench is what says so -
     * a side that is opened towards nothing is the open ending of the tube, which the models draw with the
     * plate of the size on it, and a side that faces a wall may be opened as well, because the wall may be
     * gone a moment later. The rule of {@link Pipes#connects(Block)} is the question of the transport - may
     * a pipe take fluid from what stands there - and not a question a wrench asks.
     * <p>
     * The state of the cell is written the way the generator of the models names it, so the cell redraws
     * itself with the tube of that side on it, see {@link Pipes#toggled(int, BlockFace)}.
     */
    @Override
    public boolean operateFace(World world, int x, int y, int z, BlockFace face, FaceTool tool,
            Player player, ItemStack held) {
        if (tool != FaceTool.WRENCH || Pipes.of(world.peekBlock(x, y, z)) == null) {
            return false;
        }
        world.setState(x, y, z, Pipes.toggled(world.getState(x, y, z), face));
        return true;
    }

    @Override
    protected void writeOwnData(NbtCompound data) {
        // A pipe stores nothing of its own: its connections are the state of its cell and the fluid in its
        // tube arrives with the transport of fluids.
    }

    @Override
    protected void readOwnData(NbtCompound data) {
        // Nothing was written, so there is nothing to read.
    }

    @Override
    public String toString() {
        return "PipeBlockEntity(" + type().name() + "@" + x() + "," + y() + "," + z() + ")";
    }
}
