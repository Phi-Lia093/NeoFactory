package com.philia093.neofactory.render;

import com.philia093.neofactory.entity.Entity;

/**
 * Draws one kind of {@link Entity} on top of the world.
 * <p>
 * A renderer is registered per {@link com.philia093.neofactory.entity.EntityType},
 * see {@link EntityRendererRegistry}. That keeps the knowledge of how an entity
 * looks out of the entity itself, which matters because entities never touch the
 * graphics of the game: they would otherwise not be useable in a headless test.
 * <p>
 * The caller is responsible for setting the projection matrix of the world and for
 * calling {@code begin} and {@code end} on the batch.
 */
@FunctionalInterface
public interface EntityRenderer {

    /**
     * Draws an entity.
     *
     * @param entity entity of the type this renderer was registered for
     */
    void render(Entity entity);
}
