package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.energy.LineNode;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.FaceTool;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.DiodeMachine;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.FaceClick;

/**
 * The block entity of a diode: the block a line of the power runs through, one way.
 * <p>
 * The entity is what the world asks: a line of cables that reaches this cell asks it whether it carries the
 * line through and which side it leaves by, see {@link LineNode} and {@code EnergyGrid.Cells#node}. Every
 * answer is the answer of the machine behind the block, which is a {@link DiodeMachine} and knows its tier
 * and its width - the entity itself adds the one thing a block of the game has to say and a machine cannot:
 * which clicks it takes, see {@link #operateFace}.
 * <p>
 * <b>A player aims a diode and never sets one of its sides.</b> The two sides it carries a line through are
 * what the block was built for, so the wrench turns the whole block and the clicks that would move a plug of
 * the power are refused here - the one place where a machine of the line and a diode part ways, see
 * {@code FaceConfig} and {@code MachineBlockEntity#operateFace}.
 */
public final class DiodeBlockEntity extends MachineBlockEntity implements LineNode {

    /**
     * Creates the entity of a diode.
     *
     * @param type type of this block entity
     * @param machine the diode behind this block
     */
    public DiodeBlockEntity(BlockEntityType type, DiodeMachine machine) {
        super(type, machine);
    }

    /** The diode behind this block. */
    private DiodeMachine diode() {
        return (DiodeMachine) machine();
    }

    @Override
    public BlockFace through(BlockFace from) {
        return diode().through(from);
    }

    @Override
    public Voltage voltage() {
        return diode().voltage();
    }

    @Override
    public int amperage() {
        return diode().amperage();
    }

    @Override
    public int loss() {
        return diode().loss();
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
            // The two sides a diode carries a line through are what it was built for: a player aims the whole
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
