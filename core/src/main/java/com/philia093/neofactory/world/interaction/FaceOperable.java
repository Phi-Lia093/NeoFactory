package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.world.World;

/**
 * A block that can be worked on from every one of its faces.
 * <p>
 * A player who holds a tool and looks at such a block sees a grid of nine cells on the face the eyes
 * meet, see {@link FaceGrid}: the cell under the mouse stands for one face of the block and doing
 * something to it is doing something to that face. A pipe is the first block of the game that answers
 * here - a cell of its grid opens or closes the line that runs on that side - and a machine will follow
 * with the wrench that turns it and the screwdriver that loosens its cover.
 * <p>
 * <b>Such a block is a whole cube while the grid is open.</b> A pipe is thin and a player would stand
 * inside it; the world therefore answers a whole cell for every block that shows the grid as long as one
 * of them does, see {@link World#shape(int, int, int, com.philia093.neofactory.util.Aabb)}. The rule is
 * the one the original machine mod follows, and it is what keeps a player from falling through what they
 * are working on.
 * <p>
 * A block entity implements this, because it is the part of a block that holds what the operation
 * changes - the connections of a pipe, the side a machine faces, the cover of a face.
 */
public interface FaceOperable {

    /**
     * {@code true} when this block shows the grid of faces right now.
     * <p>
     * The player still has to hold a tool for it to appear, see {@link FaceTool}; this is the question of
     * the block itself, which may refuse for a reason of its own - a machine that is not built yet, a
     * pipe whose ending stands in the open.
     *
     * @param world world the block lies in
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when the grid may be drawn on this block
     */
    boolean showsFaceGrid(World world, int x, int y, int z);

    /**
     * Does what one cell of the grid asks for.
     * <p>
     * The face handed in is the one the cell stands for, which is not always the face the grid is drawn
     * on: a player looks at the front of a pipe and opens a line that runs behind it. The tool is what
     * the operation is carried out with, so a block may do different things - or nothing at all - for the
     * wrench and for the wire cutter.
     *
     * @param world world the block lies in
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face of the block the cell stands for
     * @param tool tool the operation is carried out with, never {@link FaceTool#NONE}
     * @param player player who works on the block
     * @param held stack the player holds in that hand, never empty
     * @return {@code true} when something was done
     */
    boolean operateFace(World world, int x, int y, int z, BlockFace face, FaceTool tool,
            Player player, ItemStack held);
}
