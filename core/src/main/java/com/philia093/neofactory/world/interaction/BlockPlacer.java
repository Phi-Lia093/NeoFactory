package com.philia093.neofactory.world.interaction;

import com.badlogic.gdx.math.Vector2;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.block.state.BlockStateTable;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.BlockEntityType;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the held block into the world.
 * <p>
 * Only a block item can be built, see
 * {@link com.philia093.neofactory.item.BlockItem}, and only into an empty cell:
 * an existing block must be broken first, exactly like the original game behaves.
 * <p>
 * A build goes into the cell the player aims at, and when that cell is taken it goes into the cell
 * behind the face the line of sight entered through, see {@link BlockTarget#neighbour()}: that is how a
 * wall is built against and how a bridge grows from the block it is laid on.
 * <p>
 * <b>What carries a new block.</b> A block hangs on what it is built against, so the cell behind the
 * face of the aim is what has to be filled - a wall carries the block beside it and the ground carries
 * the block above it. A cell the mouse named carries no face, and then the rule of the view from above
 * answers instead: the ground below it has to be filled, see
 * {@link com.philia093.neofactory.world.BlockAccess#hasSupport(int, int, int,
 * com.philia093.neofactory.block.BlockFace)}. Without that difference a player could build on the ground
 * and nowhere else, which is what a world of cubes must not do. A block that would end up inside the
 * player is refused as well: the check runs on the changed world and undoes the write when the player no
 * longer fits.
 */
public final class BlockPlacer {

    /**
     * Name of the property that carries the direction a block looks in.
     * <p>
     * The model a state selects and the way it is turned are written down in
     * {@code assets/blockstates}, see {@link BlockStateTable}.
     */
    public static final String FACING = "facing";

    /**
     * Name of the property that carries the half of a cell a block fills.
     * <p>
     * A block that is thinner than a cube - the slab of stone - takes it in two halves, so the state of
     * a cell says which half it is: {@value #LOWER_HALF} or {@value #UPPER_HALF}. The game picks the half
     * while the block is built, see {@link #builtHalf}.
     */
    public static final String TYPE = "type";

    /** Value of {@link #TYPE} for the block that fills the lower half of its cell. */
    public static final String LOWER_HALF = "bottom";

    /** Value of {@link #TYPE} for the block that fills the upper half of its cell. */
    public static final String UPPER_HALF = "top";

    private static final Logger LOGGER = LogManager.getLogger();

    private BlockPlacer() {
        // Utility class: never instantiated.
    }

    /**
     * Builds one item of the held stack into the targeted cell.
     *
     * @param world world to change
     * @param player player that must stay able to move
     * @param target cell to build into
     * @param inventory inventory holding the stack the player carries
     * @return {@code true} when the world changed and one item was used up
     */
    public static boolean place(World world, Player player, BlockTarget target,
            PlayerInventory inventory) {
        if (target == null) {
            return false;
        }
        ItemStack held = inventory.heldStack();
        if (held.isEmpty()) {
            return false;
        }
        Block block = held.item().block();
        if (block == null || !block.isDrawable()) {
            // A material, a tool or a block that is never drawn, such as water.
            return false;
        }
        if (world.getBlock(target.x(), target.y(), target.z()).isAir()) {
            return buildInto(world, player, target, block, inventory, held);
        }
        // The cell is taken. A ray knows the face it entered through, so the block goes into the cell
        // behind that face: that is how a wall is built against and how a bridge grows from the block
        // it is laid on. A cell the mouse named has no face, and then there is nothing to build
        // against.
        BlockTarget behind = target.neighbour();
        return behind != null && buildInto(world, player, behind, block, inventory, held);
    }

    /**
     * Builds the held block into one empty cell.
     * <p>
     * A block needs something to stand on: a cell without a block below it is a hole in the air, and
     * the block would float. A build that would end up inside the player is refused as well - the
     * block is written first and taken back when the body no longer fits, which is the only way to ask
     * whether the body fits at all. A block the player does not fit into is a slab that was aimed at the
     * cell a body stands in, which is refused the same way.
     *
     * @param world world to change
     * @param player player that must stay able to move
     * @param cell cell to build into
     * @param block block to store
     * @param inventory inventory holding the stack the player carries
     * @param held stack the player holds
     * @return {@code true} when the world changed and one item was used up
     */
    private static boolean buildInto(World world, Player player, BlockTarget cell, Block block,
            PlayerInventory inventory, ItemStack held) {
        if (!holds(world, cell, block)) {
            return false;
        }

        world.setBlock(cell.x(), cell.y(), cell.z(), block);
        if (player.collides(world, player.position().x, player.position().z)) {
            world.setBlock(cell.x(), cell.y(), cell.z(), Blocks.AIR);
            return false;
        }
        world.setState(cell.x(), cell.y(), cell.z(), placedState(block, player, cell));

        placeBlockEntity(world, cell, block);
        useOneItem(inventory, held);
        return true;
    }

    /**
     * {@code true} when a cell is empty and carried by what the build holds on to.
     * <p>
     * A block that hangs on a side, see {@link Block#hangsOnASide()}, is only carried by a side: the floor
     * below a cell carries every other block, but a ladder that found nothing but a floor would hang in the
     * air where its rungs are, so such a build is refused. The side it hangs on is the one the aim came in
     * through, which is the same face it is turned away from.
     *
     * @param world world to look at
     * @param cell cell to test
     * @param block block that is about to be built there
     * @return {@code true} when the block may be written there
     */
    private static boolean holds(World world, BlockTarget cell, Block block) {
        if (!world.getBlock(cell.x(), cell.y(), cell.z()).isAir()) {
            return false;
        }
        BlockFace face = cell.face();
        if (block.hangsOnASide()) {
            return face != null && face.y() == 0
                    && world.hasSupport(cell.x(), cell.y(), cell.z(), face);
        }
        return world.hasSupport(cell.x(), cell.y(), cell.z(), face);
    }

    /**
     * The state a block is built with.
     * <p>
     * A block that carries no state of its own is built with zero, which is the state every property
     * at its first value. A block that does carry one answers for every property the game understands
     * while it is built:
     * <ul>
     *     <li>{@link #FACING}, the direction a block looks in, see {@link #towardsPlayer}</li>
     *     <li>{@link #TYPE}, the half of its cell a slab fills, see {@link #builtHalf}</li>
     * </ul>
     * A property a block does not carry is left out of the state, so a slab only has to say which half it
     * is and a furnace only which way it looks.
     *
     * @param block block that was built
     * @param player player that built it
     * @param cell cell the block was built into
     * @return the number of the state to store
     */
    private static int placedState(Block block, Player player, BlockTarget cell) {
        BlockStateTable table = block.states();
        Map<String, String> state = new LinkedHashMap<>();
        if (table.hasProperty(FACING)) {
            state.put(FACING, builtFacing(cell, player).toString());
        }
        if (table.hasProperty(TYPE)) {
            state.put(TYPE, builtHalf(player, cell));
        }
        return table.stateOf(state);
    }

    /**
     * The half of its cell a block of two halves is built in.
     * <p>
     * A slab fills the lower half of its cell by itself; two of them fill one cell. Which half a build
     * takes is decided by the aim, the way the original game decides it:
     * <ul>
     *     <li>an aim that went <b>up</b> into the cell - the cell a ceiling carries - is filled from the
     *         top, because that is the half a ceiling is built against</li>
     *     <li>an aim that came <b>down</b> into the cell is filled from the bottom, because that is the
     *         half the floor carries</li>
     *     <li>the side of a block carries no half of its own, so the view answers: a player who looks up
     *         builds the upper half, one who looks level or down the lower one</li>
     * </ul>
     *
     * @param player player that builds the block
     * @param cell cell the block is built into
     * @return {@link #LOWER_HALF} or {@link #UPPER_HALF}
     */
    private static String builtHalf(Player player, BlockTarget cell) {
        BlockFace face = cell.face();
        if (face == BlockFace.BOTTOM) {
            return UPPER_HALF;
        }
        if (face == BlockFace.TOP) {
            return LOWER_HALF;
        }
        return player.pitch() > 0.0f ? UPPER_HALF : LOWER_HALF;
    }

    /**
     * The direction a block is built with.
     * <p>
     * A block that hangs on the side of another one - a ladder - is turned away from that side: the face the
     * aim came in through is the side it hangs on, so that face is the direction it is built with. Every
     * other block that looks somewhere - a furnace, a workbench, an anvil - is turned towards the one who
     * built it, so the direction is the one from the block back to the player. The aim of a cell the mouse
     * named carries no face at all, and there both of them fall back to the view, see {@link #towardsPlayer}.
     *
     * @param cell cell the block was built into
     * @param player player that built it
     * @return the direction the block is built with
     */
    private static BlockFace builtFacing(BlockTarget cell, Player player) {
        BlockFace side = cell.face();
        if (side != null && side.y() == 0) {
            // The aim came in through a side of the block that carries this one: the block hangs on it.
            return side;
        }
        return towardsPlayer(player);
    }

    /**
     * The direction a block looks in when a player builds it, which is the direction towards the
     * player: a furnace shows its mouth to the one who places it, the way a sign is turned when it is
     * put up.
     *
     * @param player player that built the block
     * @return the horizontal direction from the block back to the player
     */
    private static BlockFace towardsPlayer(Player player) {
        Vector2 look = player.facing();
        if (Math.abs(look.x) >= Math.abs(look.y)) {
            // A player who looks east stands west of the block, so the block looks west.
            return look.x > 0.0f ? BlockFace.WEST : BlockFace.EAST;
        }
        return look.y > 0.0f ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    /**
     * Puts the block entity a block carries behind it.
     * <p>
     * A machine without its entity would stand there and do nothing, so the entity is
     * created while the block is built, see {@link Block#blockEntityTypeName()}. A block
     * that names a type the game does not know is reported and left without one, which
     * keeps a typo in the block table from stopping the game.
     *
     * @param world world that received the block
     * @param target cell that was built
     * @param block block that was stored there
     */
    private static void placeBlockEntity(World world, BlockTarget cell, Block block) {
        if (!block.hasBlockEntity()) {
            return;
        }
        BlockEntityType type = BlockEntityRegistry.byName(block.blockEntityTypeName());
        if (type == null) {
            LOGGER.warn("The block '{}' names the unknown block entity '{}', it stays empty",
                    block.name(), block.blockEntityTypeName());
            return;
        }
        BlockEntity entity = type.create();
        entity.setPosition(cell.x(), cell.y(), cell.z());
        world.addBlockEntity(entity);
    }

    /**
     * Takes one item out of the held stack.
     *
     * @param inventory inventory owning the stack
     * @param held stack the player holds
     */
    private static void useOneItem(PlayerInventory inventory, ItemStack held) {
        held.setCount(held.count() - 1);
        if (held.isEmpty()) {
            inventory.set(inventory.selectedSlot(), ItemStack.EMPTY);
        }
    }
}
