package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceClick;
import com.philia093.neofactory.world.interaction.FaceOperable;

/**
 * The block entity of a cable: what a line of the power looks like from the world.
 * <p>
 * The same part a pipe plays, see {@code PipeBlockEntity}: the connections of a cable are the six properties
 * of its block state, see {@link Cables#maskOf(int)}, and <b>nobody but the player changes them.</b> A cable
 * is placed with no side joined and stays that way until a wrench is put to it.
 * <p>
 * <b>The wrench works one side at a time.</b> The block answers the grid of faces of
 * {@link FaceOperable}: a player who holds the wrench sees the nine cells of the face the eyes meet, the
 * cell under the mouse stands for one side of the block, and a click opens or closes the line that runs
 * there - {@link Cables#toggled(int, BlockFace)} is the whole of the change. The mask is the state of the
 * block, so a line that is opened stays open in the world and in the save game.
 */
public class CableBlockEntity extends BlockEntity implements FaceOperable {

    /**
     * Creates the block entity of a cable.
     *
     * @param type type of the block entity
     */
    public CableBlockEntity(BlockEntityType type) {
        super(type);
    }

    /** A cable shows the grid of its faces to a player who holds a tool, whatever state it is in. */
    @Override
    public boolean showsFaceGrid(World world, int x, int y, int z) {
        return true;
    }

    /**
     * A cable does nothing by itself every tick.
     * <p>
     * Energy is not pushed by a cable the way fluid is pushed by a pipe: the net of a line is walked out of
     * the world while the machines that hang on it are ticked, see {@code EnergyGrid}, so a cable has nothing
     * to do here. It still has to say so, because the entity of a block is asked for a tick whether it wants
     * one or not.
     */
    @Override
    public void update(World world, float delta) {
        // Nothing moves in a cable by itself: the line is walked by the machines at its ends.
    }

    /**
     * Opens or closes the side of the cable the cell of the grid stands for.
     *
     * @return {@code true} when the side was changed
     */
    @Override
    public boolean operateFace(World world, int x, int y, int z, BlockFace face, FaceTool tool,
            Player player, ItemStack held, FaceClick click) {
        if (tool != FaceTool.WRENCH || !click.isRight()) {
            // A line is turned with the right button: the left one takes a block apart, and the jobs of the
            // sides belong to the machines at the ends of the line, see FaceClick.
            return false;
        }
        int state = world.getState(x, y, z);
        int turned = Cables.toggled(state, face);
        if (turned == state) {
            return false;
        }
        world.setState(x, y, z, turned);
        return true;
    }

    @Override
    public String toString() {
        return "CableBlockEntity";
    }

    /**
     * A cable stores nothing of its own.
     * <p>
     * The connections of a line are the six properties of its block state, see {@link Cables}, and the state
     * of a block is written with the chunk it stands in: there is nothing left for the entity to keep, which
     * is exactly what a pipe that holds no fluid looks like.
     */
    @Override
    protected void writeOwnData(com.philia093.neofactory.util.nbt.NbtCompound data) {
        // Nothing of a cable lives here: the mask travels in the state of its block.
    }

    @Override
    protected void readOwnData(com.philia093.neofactory.util.nbt.NbtCompound data) {
        // Nothing of a cable lives here: the mask travels in the state of its block.
    }
}
