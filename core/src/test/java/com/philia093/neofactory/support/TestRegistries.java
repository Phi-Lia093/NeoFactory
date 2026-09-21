package com.philia093.neofactory.support;

import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.BlockEntityTypes;
import com.philia093.neofactory.entity.EntityRegistry;
import com.philia093.neofactory.entity.EntityTypes;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.Items;

/**
 * Registers the game data the tests work with.
 * <p>
 * A test that creates a world or reads a save game needs the block and item tables
 * exactly like the game does, because chunks only store ids and the readers resolve
 * them through the registries. Registering happens once per JVM: both registries
 * refuse a second entry and freeze themselves afterwards.
 */
public final class TestRegistries {

    private static boolean ready;

    private TestRegistries() {
        // Utility class: never instantiated.
    }

    /** Registers blocks, items and entity types when that did not happen yet. */
    public static synchronized void ensure() {
        if (ready) {
            return;
        }
        Blocks.registerAll();
        // A fluid brings the block it stands in the world with, so its table is written
        // while the block registry is still open.
        Fluids.registerAll();
        Items.registerAll();
        EntityTypes.registerAll();
        // A stored chunk names its block entities, so the game - and every test that reads
        // a chunk - needs the types before anything is loaded.
        BlockEntityTypes.registerAll();
        BlockRegistry.freeze();
        ItemRegistry.freeze();
        EntityRegistry.freeze();
        BlockEntityRegistry.freeze();
        ready = true;
    }
}
