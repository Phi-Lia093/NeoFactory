package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;

/**
 * A block whose block entity decides what its sides are drawn with.
 * <p>
 * The picture of most blocks of the world is written once and for all: a model file names the picture of
 * every face of every box. A block that moves items, fluids or power is not like that. Which of its sides
 * carries a pipe, which one carries a cable and which one wears the plate of an exhaust is decided while
 * the game runs - a wrench puts the plug of the power on another side, a player scrolls through the faces
 * of a tank - and the art has to follow, see {@link FaceRole}.
 * <p>
 * <b>A block entity answers here.</b> The mesher of a section asks the block entity of every cell that
 * carries one, once per cell, and writes what it says over the picture its model put there, see
 * {@code SectionMesher#Appearances}. A block whose sides are its model's business does not implement this
 * at all: a chest, a pipe and a boiler keep the faces they were drawn with, and a machine of the age of
 * steam implements it the day its sides are configurable.
 * <p>
 * <b>The sides are the sides of the world.</b> The turn of a state carries the front of a machine around
 * the vertical axis, so the mesher has already applied it when it asks: what this answers is the side a
 * player is looking at and not the side a model file was written with.
 */
@FunctionalInterface
public interface FaceAppearance {

    /**
     * The two pictures one side of this block is drawn with.
     *
     * @param face side of the block, as the world has it: north is north, however the model was written
     * @return the pictures of that side, or {@code null} when the model of the block stands as it is
     */
    FacePicture pictureOn(BlockFace face);
}
