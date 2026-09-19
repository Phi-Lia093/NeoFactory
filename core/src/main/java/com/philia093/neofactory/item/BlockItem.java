package com.philia093.neofactory.item;

import com.philia093.neofactory.block.Block;

import java.util.Objects;

/**
 * An item that places a {@link Block}.
 * <p>
 * The inventory shows a block item with the picture and the colour of its block,
 * so the icon matches the tile the player sees from above. The link is also what
 * a later building system needs: placing an item turns the linked block into a
 * cell of the world, and breaking a block hands its item back to the player.
 */
public final class BlockItem extends Item {

    private final Block block;

    /**
     * Creates a block item.
     *
     * @param builder filled builder, see {@link Item#builder(int, String)}
     * @param block block this item places
     */
    public BlockItem(Builder builder, Block block) {
        super(builder, block);
        this.block = Objects.requireNonNull(block, "block");
    }

    /** Block this item places, never {@code null}. */
    @Override
    public Block block() {
        return block;
    }

    @Override
    public boolean isBlockItem() {
        return true;
    }

    @Override
    public String toString() {
        return "BlockItem(" + id() + ", " + name() + " -> " + block.name() + ")";
    }
}
