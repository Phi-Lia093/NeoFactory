package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.DiodeMachine;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceClick;

/**
 * The block entity of a diode: the machine of one direction a line ends at and another line starts at.
 * <p>
 * <b>This class adds nothing to what the machine behind the block already answers.</b> A diode is a machine of
 * two sides like a transformer is - the side of its left flank takes the power in and the side of its right one
 * hands it out - so a line of cables that reaches this cell finds what it finds at any machine: an end of that
 * line, see {@code MachineBlockEntity#updateEnergy} and {@code EnergyGrid.Cells#buffer}. What the entity of a
 * diode is for is the one thing a block of the game has to say and a machine cannot: which clicks it takes,
 * see {@link #operateFace}.
 * <p>
 * <b>A player aims a diode and never sets one of its sides.</b> The two sides it hands the power between are
 * what the block was built for, so the wrench turns the whole block and the clicks that would move a plug of
 * the power are refused here - the one place where a machine of the line and a diode part ways, see
 * {@code FaceConfig} and {@code MachineBlockEntity#operateFace}.
 */
public final class DiodeBlockEntity extends MachineBlockEntity {

    /**
     * Creates the entity of a diode.
     *
     * @param type type of this block entity
     * @param machine the diode behind this block
     */
    public DiodeBlockEntity(BlockEntityType type, DiodeMachine machine) {
        super(type, machine);
    }

    /**
     * What a tool does at a diode: the wrench turns it and nothing else touches it.
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
        if (click != FaceClick.RIGHT) {
            // The two sides a diode hands the power between are what it was built for: a player aims the whole
            // block with the wrench and never moves one of its plugs, see the note on this class.
            return false;
        }
        return super.operateFace(world, x, y, z, face, tool, player, held, click);
    }

    @Override
    public String toString() {
        return "DiodeBlockEntity(" + machine().name() + ')';
    }
}
