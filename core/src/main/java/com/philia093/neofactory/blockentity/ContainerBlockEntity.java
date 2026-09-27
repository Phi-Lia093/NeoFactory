package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.SaveTags;

/**
 * A block that holds a bag of items and nothing else.
 * <p>
 * A wall, a floor and every block of the landscape need a cell and no entity: what is there is
 * the id of the block and the state it carries. A box, a chest and the work field of a table
 * need more - the items somebody put in - and none of that fits into a cell, so it lives here,
 * next to the block and stored with the chunk that holds it, exactly like the slots of a
 * machine, see {@link MachineBlockEntity}.
 * <p>
 * <b>A container does no work.</b> Its {@link #update(World, float)} is empty, because nothing
 * about the items in it changes with the tick of the world: a chest is opened, filled and
 * emptied by a player, and the crafting field of a table is filled by a recipe that a screen
 * asks for, see {@link com.philia093.neofactory.gui.ContainerGui}. The class is therefore the
 * whole of what a block needs to keep items, and a new one - a barrel, a crate, a drawer - is
 * one subclass with the amount of slots it holds.
 * <p>
 * <b>What a container holds is stored the way a machine stores its slots</b>: the list of
 * {@code SaveTags#INVENTORY} is written and read into its group, so an empty slot keeps its
 * place, a fresh piece and a worn tool travel as they lie in the slot and an item the game no
 * longer knows is read as an empty slot instead of taking the chunk down with it.
 * <p>
 * <b>A broken container hands over what it holds.</b> The world reports a break of the block to
 * {@link #onBroken(ItemDrops, float, float)} before the block goes, so the items of a chest that
 * is taken apart fall on the ground next to it, and a player who breaks one by accident does not
 * lose what was in it.
 */
public abstract class ContainerBlockEntity extends BlockEntity {

    /** Slots of this container, never shared with another block. */
    private final Inventory contents;

    /**
     * Creates a container.
     *
     * @param type type of this block entity
     * @param slots amount of slots the block holds, at least one
     */
    protected ContainerBlockEntity(BlockEntityType type, int slots) {
        super(type);
        if (slots <= 0) {
            throw new IllegalArgumentException("A container needs at least one slot: " + slots);
        }
        this.contents = new Inventory(slots);
    }

    /** What lies in this container, the slots a screen shows. */
    public Inventory contents() {
        return contents;
    }

    /** Amount of slots this container holds. */
    public int slotCount() {
        return contents.size();
    }

    /**
     * {@code true} when nothing is in this container.
     *
     * @return {@code true} when every slot is empty
     */
    public boolean isEmpty() {
        return contents.isEmpty();
    }

    /**
     * Does one step of work.
     * <p>
     * A container does none: what lies in it only changes when a player or a screen moves it,
     * which is why the tick of the world passes it by.
     */
    @Override
    protected void update(World world, float delta) {
        // A block that holds items does nothing with them on its own.
    }

    @Override
    protected void writeOwnData(NbtCompound data) {
        data.put(SaveTags.writeInventory(contents));
    }

    @Override
    protected void readOwnData(NbtCompound data) {
        SaveTags.readInventory(contents, data.getList(SaveTags.INVENTORY));
    }

    @Override
    public void onBroken(ItemDrops drops, float worldX, float worldZ) {
        dumpContents(drops, worldX, worldZ);
    }

    /**
     * Hands everything this container holds to the world.
     * <p>
     * Called while the block is broken: without it the items of a chest would vanish with the
     * block. Every slot is emptied after its stack was reported, so a caller that hands the
     * contents over twice cannot drop the same items twice.
     *
     * @param drops sink receiving the items
     * @param worldX world X coordinate of the block
     * @param worldY world Y coordinate of the block
     */
    protected void dumpContents(ItemDrops drops, float worldX, float worldY) {
        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack stack = contents.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            drops.drop(stack, worldX, worldY);
            contents.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public String toString() {
        return "ContainerBlockEntity(" + type().name() + ", " + contents.size() + " slots)";
    }
}
