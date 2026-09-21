package com.philia093.neofactory.entity;

import com.badlogic.gdx.math.Vector2;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.Objects;
import java.util.UUID;

/**
 * Anything in the world that is not a block.
 * <p>
 * An entity owns a position in world units, a velocity and an identifier. What it
 * does each frame is up to the type, see {@link #update(World, float)}.
 * <p>
 * The save game is split in two parts, and that split is the point of this class:
 * <ul>
 *     <li>the fields every entity has - type, identifier, position, velocity - are
 *         written by {@link #writeTo(NbtCompound)} and read by
 *         {@link #readFrom(NbtCompound)}, so every entity looks the same from the
 *         outside</li>
 *     <li>whatever a type adds goes into one nested group through
 *         {@link #writeData(NbtCompound)} and {@link #readData(NbtCompound)}</li>
 * </ul>
 * A type that grows a new field therefore only touches its own group, and a world
 * stored before that still reads: every getter of a tag falls back to a default.
 * <p>
 * The identifier is not used yet, it is what keeps an entity recognizable across a
 * save game once containers, tamed animals or a second player exist.
 */
public abstract class Entity {

    private final EntityType type;
    private final UUID uuid;

    /** Position of the entity center in world units, one block is TILE_SIZE of them. */
    protected final Vector2 position = new Vector2();

    /** Velocity in world units per second. */
    protected final Vector2 velocity = new Vector2();

    /** Set once the entity left the world, checked by the entity manager. */
    private boolean removed;

    /**
     * Creates an entity with a fresh identifier.
     *
     * @param type type of this entity
     */
    protected Entity(EntityType type) {
        this(type, UUID.randomUUID());
    }

    /**
     * Creates an entity with a known identifier.
     *
     * @param type type of this entity
     * @param uuid identifier of this entity
     */
    protected Entity(EntityType type, UUID uuid) {
        this.type = Objects.requireNonNull(type, "type");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    /** Type of this entity, immutable. */
    public final EntityType type() {
        return type;
    }

    /** Identifier of this entity, stable across save games. */
    public final UUID uuid() {
        return uuid;
    }

    /** Live position of the entity center, do not mutate directly. */
    public final Vector2 position() {
        return position;
    }

    /** Live velocity of the entity, do not mutate directly. */
    public final Vector2 velocity() {
        return velocity;
    }

    /** {@code true} once the entity left the world and must be forgotten. */
    public final boolean isRemoved() {
        return removed;
    }

    /** Asks the entity manager to forget this entity after the frame. */
    public final void discard() {
        removed = true;
    }

    /**
     * Half the side length of the square box of this entity.
     * <p>
     * Used by the interaction code to find entities near the player. A type that
     * cannot be pointed at returns zero.
     *
     * @return the half extent in world units
     */
    public float hitboxHalfExtent() {
        return 0.0f;
    }

    /**
     * Writes this entity into a compound.
     * <p>
     * The shared fields are written here so that an entity of any type can be read
     * by code that only knows about entities, and the type specific part is added
     * as one nested group.
     *
     * @param target compound to fill
     */
    public final void writeTo(NbtCompound target) {
        target.putString(SaveTags.ENTITY_ID, type.name());
        target.putLong(SaveTags.UUID_MOST, uuid.getMostSignificantBits());
        target.putLong(SaveTags.UUID_LEAST, uuid.getLeastSignificantBits());
        target.putFloat(SaveTags.POS_X, position.x);
        target.putFloat(SaveTags.POS_Y, position.y);
        target.putFloat(SaveTags.VEL_X, velocity.x);
        target.putFloat(SaveTags.VEL_Y, velocity.y);

        NbtCompound data = new NbtCompound(SaveTags.ENTITY_DATA);
        writeData(data);
        target.put(data);
    }

    /**
     * Fills this entity from a compound written by {@link #writeTo(NbtCompound)}.
     * <p>
     * The type is not checked here: the reader created this entity from the stored
     * name, see {@link EntityRegistry#byName(String)}.
     *
     * @param source compound to read
     */
    public final void readFrom(NbtCompound source) {
        position.set(source.getFloat(SaveTags.POS_X, 0.0f),
                source.getFloat(SaveTags.POS_Y, 0.0f));
        velocity.set(source.getFloat(SaveTags.VEL_X, 0.0f),
                source.getFloat(SaveTags.VEL_Y, 0.0f));

        NbtCompound data = source.getCompound(SaveTags.ENTITY_DATA);
        readData(data == null ? new NbtCompound(SaveTags.ENTITY_DATA) : data);
    }

    /**
     * Writes what this type adds to the shared fields.
     *
     * @param data group to fill, created by the reader when it is missing
     */
    protected abstract void writeData(NbtCompound data);

    /**
     * Reads what this type added to the shared fields.
     *
     * @param data group to read, empty when the stored entity was written before
     *             this field existed
     */
    protected abstract void readData(NbtCompound data);

    /**
     * Advances this entity by one frame.
     *
     * @param world world the entity lives in
     * @param delta time since the last frame in seconds
     */
    public abstract void update(World world, float delta);

    @Override
    public String toString() {
        return type.name() + "@" + position.x + "," + position.y;
    }
}
