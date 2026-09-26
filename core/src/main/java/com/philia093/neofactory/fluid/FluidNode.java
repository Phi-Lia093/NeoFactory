package com.philia093.neofactory.fluid;

import com.philia093.neofactory.block.BlockFace;

/**
 * A block that carries a tank of fluid, the other end of a line of pipes.
 * <p>
 * A pipe only joins a pipe today and a machine tomorrow, see {@code Pipes#connects}: this is what a machine
 * answers with. It names, for every one of its six sides, the tank that side reaches and whether fluid may
 * enter the block there, so the transport never has to know what kind of machine stands next to it:
 * <ul>
 *     <li>{@link #tankOn(BlockFace)} - the tank a side of the block offers, which is what a pipe pours into
 *         and what a machine pours out of;</li>
 *     <li>{@link #takesOn(BlockFace)} - whether that side is a mouth, so fluid may run <i>into</i> the block
 *         there. A side that does not take is a side that only gives, and a pipe that reaches it hands
 *         nothing over, see {@code PipeBlockEntity#sidesThatGive}.</li>
 * </ul>
 * <b>A machine joins with its front and its flanks.</b> The mouth of a machine is its front - the face its
 * art shows - and fluid runs into the tank behind it; every other side of the machine is a hatch that gives
 * what the machine made, see {@code MachineBlockEntity}. That is the shape of a boiler: the water of a line
 * arrives at its mouth and the steam of its kettle leaves by the sides.
 * <p>
 * A block that answers here also names the fluid in its picture, so a pipe may be built towards it without
 * the world being asked whether it holds a machine: the block says so when it is built, see
 * {@code Block#carriesFluid()}.
 */
public interface FluidNode {

    /**
     * The tank one side of this block reaches.
     *
     * @param face side of the block
     * @return the tank, or {@code null} when that side carries none
     */
    FluidStorage tankOn(BlockFace face);

    /**
     * {@code true} when fluid may run into this block through a side.
     * <p>
     * A side that answers {@code true} is a mouth: a pipe that stands there pours what it holds into the
     * tank of that side, and a side that answers {@code false} only gives what the machine made.
     *
     * @param face side of the block
     * @return {@code true} when that side takes fluid in
     */
    boolean takesOn(BlockFace face);
}
