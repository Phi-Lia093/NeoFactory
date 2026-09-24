package com.philia093.neofactory.block;

import com.badlogic.gdx.graphics.Color;

/**
 * Declaration of every block type used by the game.
 * <p>
 * Because the world is viewed from above every block is defined by a single
 * texture, which is the face a bird would see. Ids are stable: a block keeps the
 * same id forever so that stored worlds stay readable. New blocks must always be
 * appended at the end of the list and the value of {@link #NEXT_FREE_ID} has to
 * be bumped accordingly.
 * <p>
 * <b>Solidity:</b> two flags decide whether the player can enter a cell.
 * {@code solid} means the block is an obstacle the moment it occupies a cell: a
 * wall of planks, a trunk, a canopy or {@link #BEDROCK}. {@code ground} means the
 * block can be the ground of a cell and is therefore walked over, no matter which
 * material it is made of: stone, sand and grass are hard, yet the player stands on
 * them. {@link #AIR} is ground as well, which keeps a hole the player dug passable.
 * <p>
  * A block that is neither declares itself an obstacle in every cell it fills. Forgetting
 * {@code .ground(true)} on a ground block would freeze the player, because then
 * every cell of the world would count as an obstacle, and forgetting
 * {@code .solid(true)} on a block such as {@link #PLANKS_OAK} would let the player
 * build walls to walk straight through.
 */
public final class Blocks {

    // ------------------------------------------------------------------
    // Ids. Keep the existing values unchanged and append new ones at the end.
    // ------------------------------------------------------------------

    public static final int AIR_ID = 0;
    public static final int BEDROCK_ID = 1;
    public static final int STONE_ID = 2;
    public static final int DIRT_ID = 3;
    public static final int GRASS_ID = 4;
    public static final int SAND_ID = 5;
    public static final int GRAVEL_ID = 6;
    public static final int CLAY_ID = 7;
    public static final int LOG_OAK_ID = 9;
    public static final int LEAVES_OAK_ID = 10;
    public static final int PLANKS_OAK_ID = 11;
    public static final int SANDSTONE_ID = 12;
    public static final int COAL_ORE_ID = 13;
    public static final int IRON_ORE_ID = 14;
    public static final int SNOW_ID = 15;
    public static final int TALL_GRASS_ID = 16;
    public static final int FURNACE_ID = 17;

    /** Next unused block id, used to verify that a new block got a fresh id. */
    public static final int NEXT_FREE_ID = 19;

    // ------------------------------------------------------------------
    // Block instances. They are filled by registerAll().
    // ------------------------------------------------------------------

    /** The empty block, it is never drawn and never blocks movement. */
    public static Block AIR;

    /** Unbreakable block, used at the border of the world. */
    public static Block BEDROCK;

    /** Plain stone, used by rocky biomes and as the filler of the floor layer. */
    public static Block STONE;

    /** Dirt, the main filler block of grassy biomes. */
    public static Block DIRT;

    /** Grass floor block, the ground of plains and forests. */
    public static Block GRASS;

    /** Sand, the ground of deserts and beaches. */
    public static Block SAND;

    /** Gravel, found in patches and along rocky ground. */
    public static Block GRAVEL;

    /** Clay, found in small patches of wet ground. */
    public static Block CLAY;

    /** Oak trunk, placed in the center of a tree decoration. */
    public static Block LOG_OAK;

    /** Oak leaves, the canopy blocks placed around a trunk. */
    public static Block LEAVES_OAK;

    /** Oak planks, a block the player can place in the future. */
    public static Block PLANKS_OAK;

    /** Sandstone, found below sand. */
    public static Block SANDSTONE;

    /** Coal ore, embedded in stone. */
    public static Block COAL_ORE;

    /** Iron ore, embedded in stone. */
    public static Block IRON_ORE;

    /** Snow, the ground of cold biomes. */
    public static Block SNOW;

    /** Tall grass, a decorative block of the object layer. */
    public static Block TALL_GRASS;

    /**
     * The furnace, the first machine of the game.
     * <p>
     * It is the only block so far that carries a block entity, see
     * {@link Block#blockEntityTypeName()}: the cell stores which block is there, and the
     * entity behind it holds the slots, the fire and the progress.
     */
    public static Block FURNACE;

    private Blocks() {
        // Utility class: never instantiated.
    }

    /**
     * Creates every block instance and pushes it into the {@link BlockRegistry}.
     * Must be called exactly once during startup, before any world is created.
     */
    public static void registerAll() {
        if (AIR != null) {
            return;
        }

        AIR = Block.builder(AIR_ID, "air")
                .texture(Block.NO_TEXTURE)
                .solid(false)
                .ground(true)
                .transparent(true)
                .hardness(0.0f)
                .build();
        BlockRegistry.register(AIR);

        // Impassable ground: the player may not walk onto it, which makes it the
        // block of a future world border.
        BEDROCK = Block.builder(BEDROCK_ID, "bedrock")
                .texture("bedrock")
                .solid(true)
                .hardness(-1.0f)
                .build();
        BlockRegistry.register(BEDROCK);

        // Everything from here down to the ores is a ground surface: the player
        // walks over it, so it is ground. A placed block of the same kind stands in
        // the object layer and blocks the way there.
        STONE = Block.builder(STONE_ID, "stone")
                .texture("stone")
                .ground(true)
                .hardness(1.5f)
                .harvestLevel(1)
                .build();
        BlockRegistry.register(STONE);

        DIRT = Block.builder(DIRT_ID, "dirt")
                .texture("dirt")
                .ground(true)
                .hardness(0.5f)
                .build();
        BlockRegistry.register(DIRT);

        // grass_top.png is a grey scale sheet in the style of Minecraft, the biome
        // colour is applied at draw time through the tint.
        GRASS = Block.builder(GRASS_ID, "grass")
                .texture("grass_top")
                .faces(FaceSet.builder()
                        .all("grass_side")
                        .top("grass_top")
                        .bottom("dirt")
                        .build())
                .ground(true)
                .tint(new Color(0.60f, 0.80f, 0.36f, 1.0f))
                .hardness(0.6f)
                .build();
        BlockRegistry.register(GRASS);

        SAND = Block.builder(SAND_ID, "sand")
                .texture("sand")
                .ground(true)
                .hardness(0.5f)
                .build();
        BlockRegistry.register(SAND);

        GRAVEL = Block.builder(GRAVEL_ID, "gravel")
                .texture("gravel")
                .ground(true)
                .hardness(0.6f)
                .build();
        BlockRegistry.register(GRAVEL);

        CLAY = Block.builder(CLAY_ID, "clay")
                .texture("clay")
                .ground(true)
                .hardness(0.6f)
                .build();
        BlockRegistry.register(CLAY);

        // Seen from above an oak trunk shows its growth rings, which makes the
        // center of a tree clearly distinguishable from its leaves.
        LOG_OAK = Block.builder(LOG_OAK_ID, "log_oak")
                .texture("log_oak_top")
                .faces(FaceSet.builder()
                        .all("log_oak")
                        .top("log_oak_top")
                        .bottom("log_oak_top")
                        .build())
                .solid(true)
                .hardness(2.0f)
                .build();
        BlockRegistry.register(LOG_OAK);

        // The leaf texture is grey scale as well, the tint turns it into an oak
        // canopy.
        LEAVES_OAK = Block.builder(LEAVES_OAK_ID, "leaves_oak")
                .texture("leaves_oak")
                .solid(true)
                .tint(new Color(0.62f, 1.0f, 0.42f, 1.0f))
                .transparent(true)
                .hardness(0.2f)
                .build();
        BlockRegistry.register(LEAVES_OAK);

        PLANKS_OAK = Block.builder(PLANKS_OAK_ID, "planks_oak")
                .texture("planks_oak")
                .solid(true)
                .hardness(2.0f)
                .build();
        BlockRegistry.register(PLANKS_OAK);

        SANDSTONE = Block.builder(SANDSTONE_ID, "sandstone")
                .texture("sandstone_top")
                .ground(true)
                .hardness(0.8f)
                .harvestLevel(1)
                .build();
        BlockRegistry.register(SANDSTONE);

        // Ores are ground as well: the rocky biome replaces its stone floor with
        // them, so they must stay walkable.
        COAL_ORE = Block.builder(COAL_ORE_ID, "coal_ore")
                .texture("coal_ore")
                .ground(true)
                .hardness(3.0f)
                .harvestLevel(1)
                .build();
        BlockRegistry.register(COAL_ORE);

        IRON_ORE = Block.builder(IRON_ORE_ID, "iron_ore")
                .texture("iron_ore")
                .ground(true)
                .hardness(3.0f)
                .harvestLevel(1)
                .build();
        BlockRegistry.register(IRON_ORE);

        SNOW = Block.builder(SNOW_ID, "snow")
                .texture("snow")
                .ground(true)
                .hardness(0.4f)
                .build();
        BlockRegistry.register(SNOW);

        // Like the ground the blades are grey scale and receive their colour from
        // the tint. A player may place them into the ground as well, so they are
        // ground: a tuft of grass is never a wall.
        TALL_GRASS = Block.builder(TALL_GRASS_ID, "tall_grass")
                .texture("tallgrass")
                .tint(new Color(0.75f, 0.95f, 0.45f, 1.0f))
                .solid(false)
                .ground(true)
                .transparent(true)
                .hardness(0.1f)
                .build();
        BlockRegistry.register(TALL_GRASS);

        // The first machine of the game. Its block stands in the object layer like a wall
        // does, and it names the block entity that holds what the cell cannot: the slots,
        // the fuel and the work that is done, see BlockEntities.FURNACE.
        FURNACE = Block.builder(FURNACE_ID, "furnace")
                .texture("furnace_top")
                .solid(true)
                .hardness(3.5f)
                .harvestLevel(1)
                .blockEntity("furnace")
                .build();
        BlockRegistry.register(FURNACE);

        BlockRegistry.freeze();
    }
}
