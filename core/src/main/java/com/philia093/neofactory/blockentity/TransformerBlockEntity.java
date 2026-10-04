package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.TransformerMachine;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceClick;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The block entity of a transformer: the machine that joins two ages of the line of the power.
 * <p>
 * Everything the machine does is what a {@link TransformerMachine} does - two buffers of energy and a tick
 * that hands the energy of one to the other - and the entity adds the one thing a block of the world has to
 * answer for itself: <b>what a tool does at it</b>. A player knocks on a transformer with a <b>mallet</b> to
 * say which of its two ends takes the power in, and the wrench turns the whole block the way it turns every
 * machine of the line, see {@code MachineBlockEntity#operateFace}.
 * <p>
 * <b>The sides of a transformer are what the block was built for.</b> One side of it carries the high voltage
 * and five carry the low one, and no click of a player moves one of them: the clicks that would set a side of
 * the power are refused here, which is the one way a transformer and a machine of the usual kind part, see
 * {@code FaceConfig}.
 */
public final class TransformerBlockEntity extends MachineBlockEntity {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates the entity of a transformer.
     *
     * @param type type of this block entity
     * @param machine the transformer behind this block
     */
    public TransformerBlockEntity(BlockEntityType type, TransformerMachine machine) {
        super(type, machine);
    }

    /** The transformer behind this block. */
    public TransformerMachine transformer() {
        return (TransformerMachine) machine();
    }

    /**
     * What a tool does at a transformer: the mallet turns it around and the wrench turns the block.
     *
     * @param world world this block lies in
     * @param x x of the block
     * @param y y of the block
     * @param z z of the block
     * @param face side of the block that was clicked
     * @param tool tool the player holds
     * @param player player that clicked
     * @param held stack the player holds
     * @param click which button was used and whether a modifier was held
     * @return {@code true} when the block changed
     */
    @Override
    public boolean operateFace(World world, int x, int y, int z, BlockFace face, FaceTool tool, Player player,
            ItemStack held, FaceClick click) {
        if (tool == FaceTool.MALLET && click == FaceClick.RIGHT) {
            transformer().toggle();
            // The arrows of every side and the colour of the two terminals change with the end that takes the
            // power in, so the section is asked to be meshed again, see World#markDirty.
            world.markDirty(x, y, z);
            LOGGER.info("The {} at ({}, {}, {}) now steps {} ", transformer().name(), x, y, z,
                    transformer().isSteppingDown() ? "down" : "up");
            return true;
        }
        if (click != FaceClick.RIGHT) {
            // The sides of a transformer are what the block was built for: a player turns the whole block and
            // never moves one of its ends, see the note on this class.
            return false;
        }
        return super.operateFace(world, x, y, z, face, tool, player, held, click);
    }

    @Override
    public String toString() {
        return "TransformerBlockEntity(" + transformer() + ')';
    }
}
