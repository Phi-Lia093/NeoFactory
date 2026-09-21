package com.philia093.neofactory.entity;

import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.SaveTags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Every entity of a world.
 * <p>
 * The manager owns the list, advances it and takes care of what a save game needs:
 * {@link #save()} turns the list into tag data and {@link #load(NbtList, World)}
 * turns tag data back into entities, see {@link Entity} for the layout.
 * <p>
 * The list belongs to the world and not to a chunk. Entities are therefore never
 * bound to loaded terrain: a dropped item stays where it is while the player walks
 * around, and a chunk that is dropped from memory never leaves an entity dangling.
 * Storing entities next to the chunk they stand in is the next step, which is what
 * keeps a long session from collecting entities of places the player left.
 * <p>
 * Only entities close to the player are advanced, so a world with many entities
 * still costs what the player can see.
 */
public final class EntityManager {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Distance in blocks an entity has to be within to be advanced. */
    private static final float ACTIVE_RANGE_BLOCKS = 128.0f;

    private final List<Entity> entities = new ArrayList<>();

    /** Every entity, in spawn order, do not mutate directly. */
    public List<Entity> all() {
        return Collections.unmodifiableList(entities);
    }

    /** Amount of entities currently in the world. */
    public int count() {
        return entities.size();
    }

    /**
     * Adds an entity to the world.
     *
     * @param entity entity to add
     */
    public void spawn(Entity entity) {
        entities.add(entity);
    }

    /**
     * Removes an entity right away.
     *
     * @param entity entity to remove
     * @return {@code true} when the entity was part of the world
     */
    public boolean remove(Entity entity) {
        return entities.remove(entity);
    }

    /** Removes every entity, used when a world is thrown away. */
    public void clear() {
        entities.clear();
    }

    /**
     * The player of this world.
     * <p>
     * A world holds one player, which is what makes this lookup worth having: the
     * screen asks for it instead of keeping its own copy, so the player survives a
     * save and a load like any other entity.
     *
     * @return the player, or {@code null} when the world has none yet
     */
    public Player player() {
        for (Entity entity : entities) {
            if (entity.type() == EntityTypes.PLAYER) {
                return (Player) entity;
            }
        }
        return null;
    }

    /**
     * Every entity within a distance of a position.
     *
     * @param worldX world X coordinate to measure from
     * @param worldY world Y coordinate to measure from
     * @param radius distance in world units
     * @return the entities inside that circle, in spawn order
     */
    public List<Entity> near(float worldX, float worldY, float radius) {
        List<Entity> found = new ArrayList<>();
        float radiusSquared = radius * radius;
        for (Entity entity : entities) {
            if (entity.position().dst2(worldX, worldY) <= radiusSquared) {
                found.add(entity);
            }
        }
        return found;
    }

    /**
     * Advances every entity close enough to the player and forgets the ones that
     * asked to be discarded.
     * <p>
     * The player is advanced as well, which is why the movement keys have to be
     * applied before this call. It is never skipped, because the player is by
     * definition where the player stands.
     *
     * @param world world the entities live in
     * @param delta time since the last frame in seconds
     * @param playerBlockX block X coordinate of the player
     * @param playerBlockY block Y coordinate of the player
     */
    public void update(World world, float delta, float playerBlockX, float playerBlockY) {
        float tile = Constants.TILE_SIZE;
        float rangeSquared = ACTIVE_RANGE_BLOCKS * tile * ACTIVE_RANGE_BLOCKS * tile;
        float centerX = playerBlockX * tile;
        float centerY = playerBlockY * tile;

        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity.isRemoved()) {
                iterator.remove();
                continue;
            }
            boolean far = entity.position().dst2(centerX, centerY) > rangeSquared;
            if (!far || entity.type() == EntityTypes.PLAYER) {
                entity.update(world, delta);
            }
            if (entity.isRemoved()) {
                iterator.remove();
            }
        }
    }

    /**
     * Turns every entity into tag data.
     *
     * @return the list to store, empty when the world holds no entity
     */
    public NbtList save() {
        NbtList list = new NbtList(SaveTags.ENTITIES);
        for (Entity entity : entities) {
            NbtCompound entry = new NbtCompound("");
            entity.writeTo(entry);
            list.add(entry);
        }
        return list;
    }

    /**
     * Creates entities from tag data.
     * <p>
     * A type that is not known is reported and skipped, so a single unknown entity
     * cannot keep a whole world from opening.
     *
     * @param list stored entities, may be {@code null}
     * @param world world the entities live in
     * @return amount of entities that were created
     */
    public int load(NbtList list, World world) {
        if (list == null) {
            return 0;
        }
        int loaded = 0;
        for (int index = 0; index < list.size(); index++) {
            NbtCompound entry = list.getCompound(index);
            String name = entry.getString(SaveTags.ENTITY_ID, "");
            EntityType type = EntityRegistry.byName(name);
            if (type == null) {
                LOGGER.warn("Entity {} of the save game has the unknown type '{}' and is skipped",
                        index, name);
                continue;
            }
            Entity entity = type.create();
            entity.readFrom(entry);
            entities.add(entity);
            loaded++;
        }
        return loaded;
    }

    @Override
    public String toString() {
        return "EntityManager(" + entities.size() + " entities)";
    }
}
