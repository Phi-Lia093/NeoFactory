package com.philia093.neofactory.render;

import com.philia093.neofactory.entity.Entity;
import com.philia093.neofactory.entity.EntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws every entity of a world, one renderer per type.
 * <p>
 * The registry is filled while the screen is set up and asked to draw once per
 * frame, which keeps the game loop free of a growing chain of {@code if} checks.
 * A type without a renderer is skipped instead of breaking the frame: adding an
 * entity type that nobody draws yet should not be visible as an error.
 */
public final class EntityRendererRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<EntityType, EntityRenderer> renderers = new LinkedHashMap<>();

    /**
     * Adds the renderer of a type.
     *
     * @param type type to draw
     * @param renderer renderer of that type, replaces one that is already there
     */
    public void register(EntityType type, EntityRenderer renderer) {
        renderers.put(type, renderer);
    }

    /**
     * {@code true} when a type has a renderer.
     *
     * @param type type to look up
     */
    public boolean has(EntityType type) {
        return renderers.containsKey(type);
    }

    /** Amount of registered renderers. */
    public int count() {
        return renderers.size();
    }

    /**
     * Draws every entity that has a renderer.
     *
     * @param entities entities to draw, in the order they should appear
     */
    public void render(Iterable<Entity> entities) {
        for (Entity entity : entities) {
            EntityRenderer renderer = renderers.get(entity.type());
            if (renderer == null) {
                LOGGER.debug("No renderer for entity type {}", entity.type().name());
                continue;
            }
            renderer.render(entity);
        }
    }
}
