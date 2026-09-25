package com.philia093.neofactory.support;

import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.block.model.ModelLoader;
import com.philia093.neofactory.block.model.ModelRegistry;
import com.philia093.neofactory.block.state.BlockStateLoader;
import com.philia093.neofactory.block.state.BlockStateRegistry;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.BlockEntityTypes;
import com.philia093.neofactory.entity.EntityRegistry;
import com.philia093.neofactory.entity.EntityTypes;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.loot.LootTableLoader;

import java.nio.file.Path;

/**
 * Registers the game data the tests work with.
 * <p>
 * A test that creates a world or reads a save game needs the block and item tables
 * exactly like the game does, because chunks only store ids and the readers resolve
 * them through the registries. Registering happens once per JVM: both registries
 * refuse a second entry and freeze themselves afterwards.
 * <p>
 * The models and the states of the blocks are read from the assets, which is what a test of the
 * mesher or of the pictures of the world needs; the files are read with plain file access, because a
 * test runs without a window and without the libGDX file system.
 */
public final class TestRegistries {

    /** Root of the assets, the tests run inside the core module. */
    public static final Path ASSETS = Path.of("..", "assets");

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
        // The shape of a block and the states it may take live in the assets, so a test that meshes a
        // section or looks at a picture of the world reads them the way the game does.
        ModelLoader.loadAllFrom(ASSETS);
        ModelRegistry.freeze();
        BlockStateLoader.loadAllFrom(ASSETS);
        BlockStateRegistry.freeze();
        // What a broken block leaves behind is asked of its loot table, and a block that names none hands
        // over itself, see LootTableLoader: a test that breaks a block reads the tables of the game.
        LootTableLoader.loadAllFrom(ASSETS);
        ready = true;
    }
}

