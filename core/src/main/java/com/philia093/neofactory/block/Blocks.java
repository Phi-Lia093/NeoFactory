package com.philia093.neofactory.block;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.item.ToolType;

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
    public static final int LOG_OAK_ID = 8;
    public static final int LEAVES_OAK_ID = 9;
    public static final int PLANKS_OAK_ID = 10;
    public static final int SANDSTONE_ID = 11;
    public static final int COAL_ORE_ID = 12;
    public static final int IRON_ORE_ID = 13;
    public static final int FURNACE_ID = 14;

    // ------------------------------------------------------------------
    // The blocks that came with the third axis: the stone family, the plants and the workshop.
    // The art folder decides which blocks exist - a picture the pack holds and no block names is
    // art nothing draws, which is what the texture audit reports, see TextureAuditTest.
    // ------------------------------------------------------------------

    public static final int COBBLESTONE_ID = 15;
    public static final int STONE_BRICK_ID = 16;
    public static final int STONE_SLAB_ID = 17;
    public static final int BRICK_ID = 18;
    public static final int GLASS_ID = 19;
    public static final int GLOWSTONE_ID = 20;
    public static final int OBSIDIAN_ID = 21;
    public static final int SAPLING_OAK_ID = 22;
    public static final int CRAFTING_TABLE_ID = 23;
    public static final int ANVIL_ID = 24;
    public static final int CAULDRON_ID = 25;
    public static final int TORCH_ID = 26;
    public static final int LADDER_ID = 27;

    // ------------------------------------------------------------------
    // The machines of the industry. They are appended like every other block, so a cell of a stored world
    // keeps the meaning it had.
    // ------------------------------------------------------------------

    /** The bronze boiler, the machine that turns water into steam. */
    public static final int BRONZE_BOILER_ID = 28;

    /** Next unused block id, used to verify that a new block got a fresh id. */
    public static final int NEXT_FREE_ID = 29;

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

    /** Cobblestone, the stone a player cuts out of a hill. */
    public static Block COBBLESTONE;

    /** Stone bricks, cut and laid out. */
    public static Block STONE_BRICK;

    /** A slab of stone: the first block that is half a cube, and the first that a body steps onto. */
    public static Block STONE_SLAB;

    /** Bricks, fired out of clay. */
    public static Block BRICK;

    /** Glass, the first block the eye looks through. */
    public static Block GLASS;

    /** Glowstone, the glowing rock of the nether. */
    public static Block GLOWSTONE;

    /** Obsidian, the hardest block of the pack. */
    public static Block OBSIDIAN;

    /** An oak sapling, a plant of the object layer. */
    public static Block SAPLING_OAK;

    /** The crafting table, the front of a workshop. */
    public static Block CRAFTING_TABLE;

    /** The anvil, whose top wears out with every use. */
    public static Block ANVIL;

    /** The cauldron, the first block with an inside. */
    public static Block CAULDRON;

    /** A torch, a small block that burns in a cell the player walks through. */
    public static Block TORCH;

    /** The ladder, a thin block that hangs on a wall. */
    public static Block LADDER;

    /**
     * The furnace, the first machine of the game.
     * <p>
     * It is the only block so far that carries a block entity, see
     * {@link Block#blockEntityTypeName()}: the cell stores which block is there, and the
     * entity behind it holds the slots, the fire and the progress.
     */
    public static Block FURNACE;

    /** The bronze boiler block, the machine that turns water into steam. */
    public static Block BRONZE_BOILER;

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
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(STONE);

        DIRT = Block.builder(DIRT_ID, "dirt")
                .texture("dirt")
                .ground(true)
                .hardness(0.5f)
                .toolType(ToolType.SHOVEL)
                .build();
        BlockRegistry.register(DIRT);

        // grass_top.png is a grey scale sheet in the style of Minecraft, the biome
        // colour is applied at draw time through the tint.
        // The picture of the top is what a slot and a hand show; the six faces of the block are
        // written down in models/block/grass.json, the layer of the biome colour included.
        GRASS = Block.builder(GRASS_ID, "grass")
                .texture("grass_top")
                .ground(true)
                .tint(new Color(0.60f, 0.80f, 0.36f, 1.0f))
                .hardness(0.6f)
                .toolType(ToolType.SHOVEL)
                .build();
        BlockRegistry.register(GRASS);

        SAND = Block.builder(SAND_ID, "sand")
                .texture("sand")
                .ground(true)
                .hardness(0.5f)
                .toolType(ToolType.SHOVEL)
                .build();
        BlockRegistry.register(SAND);

        GRAVEL = Block.builder(GRAVEL_ID, "gravel")
                .texture("gravel")
                .ground(true)
                .hardness(0.6f)
                .toolType(ToolType.SHOVEL)
                .build();
        BlockRegistry.register(GRAVEL);

        CLAY = Block.builder(CLAY_ID, "clay")
                .texture("clay")
                .ground(true)
                .hardness(0.6f)
                .toolType(ToolType.SHOVEL)
                .build();
        BlockRegistry.register(CLAY);

        // Seen from above an oak trunk shows its growth rings, which makes the
        // center of a tree clearly distinguishable from its leaves.
        // The bark name is what a slot shows; the rings of the ends live in
        // models/block/log_oak.json, which keeps the top of a trunk clearly distinguishable
        // from its leaves.
        LOG_OAK = Block.builder(LOG_OAK_ID, "log_oak")
                .texture("log_oak_top")
                .solid(true)
                .hardness(2.0f)
                .toolType(ToolType.AXE)
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
                .toolType(ToolType.HOE)
                .build();
        BlockRegistry.register(LEAVES_OAK);

        PLANKS_OAK = Block.builder(PLANKS_OAK_ID, "planks_oak")
                .texture("planks_oak")
                .solid(true)
                .hardness(2.0f)
                .toolType(ToolType.AXE)
                .build();
        BlockRegistry.register(PLANKS_OAK);

        SANDSTONE = Block.builder(SANDSTONE_ID, "sandstone")
                .texture("sandstone_top")
                .ground(true)
                .hardness(0.8f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(SANDSTONE);

        // Ores are ground as well: the rocky biome replaces its stone floor with
        // them, so they must stay walkable.
        COAL_ORE = Block.builder(COAL_ORE_ID, "coal_ore")
                .texture("coal_ore")
                .ground(true)
                .hardness(3.0f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(COAL_ORE);

        IRON_ORE = Block.builder(IRON_ORE_ID, "iron_ore")
                .texture("iron_ore")
                .ground(true)
                .hardness(3.0f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(IRON_ORE);

        // The first machine of the game. Its block stands in the object layer like a wall
        // does, and it names the block entity that holds what the cell cannot: the slots,
        // the fuel and the work that is done, see BlockEntities.FURNACE.
        FURNACE = Block.builder(FURNACE_ID, "furnace")
                .texture("furnace/furnace_top")
                .solid(true)
                .hardness(3.5f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .blockEntity("furnace")
                .build();
        BlockRegistry.register(FURNACE);

        // ------------------------------------------------------------------
        // The stone family. Cobblestone, stone bricks and bricks are whole cubes of one picture;
        // the slab is the first block that is half a cube, so its shape lives in
        // models/block/stone_slab.json. All of them are ground: a player walks over them.
        // ------------------------------------------------------------------
        COBBLESTONE = Block.builder(COBBLESTONE_ID, "cobblestone")
                .texture("cobblestone")
                .ground(true)
                .hardness(2.0f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(COBBLESTONE);

        STONE_BRICK = Block.builder(STONE_BRICK_ID, "stonebrick")
                .texture("stonebrick")
                .ground(true)
                .hardness(1.5f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(STONE_BRICK);

        // A slab is half a block high, so the player steps onto it instead of walking into it: the shape
        // of models/block/stone_slab.json is both what is drawn and what a body runs into, and the state
        // of a cell says which half it fills - a slab aimed at a ceiling fills the upper one.
        STONE_SLAB = Block.builder(STONE_SLAB_ID, "stone_slab")
                .texture("stone_slab_top")
                .ground(true)
                .hardness(1.5f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(STONE_SLAB);

        BRICK = Block.builder(BRICK_ID, "brick")
                .texture("brick")
                .ground(true)
                .hardness(2.0f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(BRICK);

        // Glass is a block the eye sees through: it is transparent like the leaves and culls
        // nothing, which is why a window never hides the world behind it.
        GLASS = Block.builder(GLASS_ID, "glass")
                .texture("glass")
                .ground(true)
                .transparent(true)
                .hardness(0.3f)
                .build();
        BlockRegistry.register(GLASS);

        GLOWSTONE = Block.builder(GLOWSTONE_ID, "glowstone")
                .texture("glowstone")
                .ground(true)
                .hardness(0.3f)
                .build();
        BlockRegistry.register(GLOWSTONE);

        OBSIDIAN = Block.builder(OBSIDIAN_ID, "obsidian")
                .texture("obsidian")
                .ground(true)
                .hardness(50.0f)
                .harvestLevel(3)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(OBSIDIAN);

        // ------------------------------------------------------------------
        // The plants of the object layer. A sapling is not solid and not a wall: the player walks
        // through it, which is what keeps a meadow passable. Its shape is the cross of
        // models/block/sapling_oak.json.
        // ------------------------------------------------------------------
        SAPLING_OAK = Block.builder(SAPLING_OAK_ID, "sapling_oak")
                .texture("sapling_oak")
                .solid(false)
                .ground(true)
                .transparent(true)
                .hardness(0.0f)
                .build();
        BlockRegistry.register(SAPLING_OAK);

        // ------------------------------------------------------------------
        // The workshop: the table where things are built, the anvil that wears out and the cauldron
        // with an inside. Their shapes are the files of models/block; the table is turned by the states
        // of assets/blockstates/crafting_table.json, like the furnace.
        // ------------------------------------------------------------------
        CRAFTING_TABLE = Block.builder(CRAFTING_TABLE_ID, "crafting_table")
                .texture("crafting_table/crafting_table_top")
                .ground(true)
                .hardness(2.5f)
                .toolType(ToolType.AXE)
                .build();
        BlockRegistry.register(CRAFTING_TABLE);

        ANVIL = Block.builder(ANVIL_ID, "anvil")
                .texture("anvil/anvil_top")
                .ground(true)
                .hardness(5.0f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(ANVIL);

        CAULDRON = Block.builder(CAULDRON_ID, "cauldron")
                .texture("cauldron/cauldron_top")
                .ground(true)
                .hardness(2.0f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .build();
        BlockRegistry.register(CAULDRON);

        TORCH = Block.builder(TORCH_ID, "torch")
                .texture("torch_on")
                .solid(false)
                .ground(true)
                .transparent(true)
                .hardness(0.0f)
                .build();
        BlockRegistry.register(TORCH);

        LADDER = Block.builder(LADDER_ID, "ladder")
                .texture("ladder")
                .solid(false)
                .ground(true)
                .transparent(true)
                .climbable(true)
                .hangsOnASide(true)
                .hardness(0.4f)
                .toolType(ToolType.AXE)
                .build();
        BlockRegistry.register(LADDER);

        // ------------------------------------------------------------------
        // The machines of the industry. A machine is a whole cube that carries a block entity: the cell
        // says which machine stands there and which way it looks, and the entity holds what the cell
        // cannot - the slots, the fuel, the tanks and the work, see MachineBlockEntity.
        // ------------------------------------------------------------------
        BRONZE_BOILER = Block.builder(BRONZE_BOILER_ID, "bronze_boiler")
                .texture("bronze_boiler/bronze_boiler_front")
                .solid(true)
                .hardness(3.5f)
                .harvestLevel(1)
                .toolType(ToolType.PICKAXE)
                .blockEntity("bronze_boiler")
                .build();
        BlockRegistry.register(BRONZE_BOILER);

        BlockRegistry.freeze();
    }
}
