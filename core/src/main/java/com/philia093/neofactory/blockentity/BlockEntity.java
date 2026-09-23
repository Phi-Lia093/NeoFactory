package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;

import java.util.Objects;

/**
 * The data a block carries beyond its id and its state.
 * <p>
 * A chunk stores two numbers per cell - what block is there and what state it has - and
 * that is all a wall, a floor or a pipe needs. A machine needs more: the slots it holds,
 * the energy in its buffer, the fluid in its tanks, how far its work has come. None of
 * that fits into a cell, so it lives here, next to the block and stored with the chunk
 * that holds it, see {@link com.philia093.neofactory.world.save.ChunkCodec}.
 * <p>
 * The class is written the way {@link com.philia093.neofactory.entity.Entity} is: what
 * every block entity has - its type and its position - is kept here, and whatever a type
 * adds goes into one nested group through {@link #writeData(NbtCompound)}. A type that
 * grows a field therefore only touches its own group.
 * <p>
 * A block entity is created by its {@link BlockEntityType} and put into the world by
 * {@link World#addBlockEntity(BlockEntity)}, which is also what tells it where it lies.
 */
public abstract class BlockEntity {

    private final BlockEntityType type;
    private int x;
    private int y;
    private int z;

    /**
     * Creates a block entity.
     *
     * @param type type of this block entity
     */
    protected BlockEntity(BlockEntityType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    /** Type of this block entity, immutable. */
    public final BlockEntityType type() {
        return type;
    }

    /** Block X coordinate this entity lies at. */
    public final int x() {
        return x;
    }

    /** Block Y coordinate this entity lies at, its height in the world. */
    public final int y() {
        return y;
    }

    /** Block Z coordinate this entity lies at. */
    public final int z() {
        return z;
    }

    /**
     * Remembers where this entity lies.
     * <p>
     * Called by the world while it places the entity and by the reader while it fills a
     * chunk, so nothing else has to set a position.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     */
    public final void setPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** {@code true} when this entity lies at a cell. */
    public final boolean isAt(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    /**
     * Advances this entity by one tick.
     *
     * @param world world this entity lies in
     * @param delta time since the last tick in seconds, always positive
     */
    public final void tick(World world, float delta) {
        if (delta > 0.0f) {
            update(world, delta);
        }
    }

    /**
     * Writes this entity into a group.
     * <p>
     * The type and the position are written by the chunk that stores this entity, so a
     * type only has to add its own fields.
     *
     * @param data group to fill
     */
    public final void writeData(NbtCompound data) {
        writeOwnData(data);
    }

    /**
     * Fills this entity from a group written by {@link #writeData(NbtCompound)}.
     * <p>
     * The position is already set when this is called, so an entity of any type may look
     * at its neighbours while it loads.
     *
     * @param data group to read, never {@code null}
     */
    public final void readData(NbtCompound data) {
        readOwnData(data);
    }

    /**
     * Hands the content of this entity to the world while its block is broken.
     * <p>
     * Without this the items of a machine would vanish with the block. The default does
     * nothing, because most block entities hold nothing a player could pick up.
     *
     * @param drops sink receiving the items
     * @param worldX world X coordinate of the block
     * @param worldZ world Z coordinate of the block
     */
    public void onBroken(ItemDrops drops, float worldX, float worldZ) {
        // A block entity without content leaves nothing behind.
    }

    /**
     * Does one step of work.
     *
     * @param world world this entity lies in
     * @param delta time since the last tick in seconds, always positive
     */
    protected abstract void update(World world, float delta);

    /**
     * Writes what this type adds to the shared fields.
     *
     * @param data group to fill
     */
    protected abstract void writeOwnData(NbtCompound data);

    /**
     * Reads what this type added to the shared fields.
     *
     * @param data group to read, empty when nothing was stored
     */
    protected abstract void readOwnData(NbtCompound data);

    @Override
    public String toString() {
        return type.name() + "@" + x + "," + y + "," + z;
    }
}
