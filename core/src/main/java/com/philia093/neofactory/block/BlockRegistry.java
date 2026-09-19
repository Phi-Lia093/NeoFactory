package com.philia093.neofactory.block;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global lookup table for every {@link Block} type known to the game.
 * <p>
 * Blocks are registered once during startup through {@link Blocks#registerAll()}.
 * Chunks only store the numeric {@link Block#id()}, therefore the id assigned to
 * a block must never change between save games.
 */
public final class BlockRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Maximum number of block types. The limit exists because chunks store the
     * block id inside a single byte per block.
     */
    public static final int MAX_BLOCKS = 256;

    private static final List<Block> BY_ID = new ArrayList<>(MAX_BLOCKS);
    private static final Map<String, Block> BY_NAME = new HashMap<>();
    private static boolean frozen = false;

    private BlockRegistry() {
        // Utility class: never instantiated.
    }

    /**
     * Adds a block to the registry.
     *
     * @param block block to register
     * @throws IllegalStateException when the registry was already frozen or when
     *         the id or the name is used twice
     */
    public static void register(Block block) {
        if (frozen) {
            throw new IllegalStateException("Block registry is frozen, cannot add " + block.name());
        }
        if (block.id() < 0 || block.id() >= MAX_BLOCKS) {
            throw new IllegalArgumentException("Block id out of range: " + block.id());
        }
        if (BY_NAME.containsKey(block.name())) {
            throw new IllegalArgumentException("Duplicate block name: " + block.name());
        }
        while (BY_ID.size() <= block.id()) {
            BY_ID.add(null);
        }
        if (BY_ID.get(block.id()) != null) {
            throw new IllegalArgumentException("Duplicate block id: " + block.id());
        }
        BY_ID.set(block.id(), block);
        BY_NAME.put(block.name(), block);
    }

    /** Prevents any further registration, called once startup is finished. */
    public static void freeze() {
        frozen = true;
        LOGGER.info("Block registry frozen with {} block types", BY_NAME.size());
    }

    /**
     * Returns the block with the given id.
     *
     * @param id numeric block id
     * @return the matching block or {@link Blocks#AIR} when the id is unused
     */
    public static Block byId(int id) {
        if (id < 0 || id >= BY_ID.size()) {
            return Blocks.AIR;
        }
        Block block = BY_ID.get(id);
        return block == null ? Blocks.AIR : block;
    }

    /**
     * Returns the block with the given name.
     *
     * @param name block name such as {@code "stone"}
     * @return the matching block or {@code null} when no block uses that name
     */
    public static Block byName(String name) {
        return BY_NAME.get(name);
    }

    /** Returns every registered block, including air. */
    public static List<Block> all() {
        List<Block> result = new ArrayList<>(BY_NAME.size());
        for (Block block : BY_ID) {
            if (block != null) {
                result.add(block);
            }
        }
        return result;
    }

    /** Amount of registered block types. */
    public static int count() {
        return BY_NAME.size();
    }
}