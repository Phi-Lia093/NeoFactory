package com.philia093.neofactory.item;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.pipe.Pipes;

/**
 * Declaration of every item type used by the game.
 * <p>
 * The list is split into four groups: the items that place a block, the raw
 * materials, the food and the unique items such as tools. Ids are
 * stable, so new items must always be appended at the end and the value of
 * {@link #NEXT_FREE_ID} has to be bumped accordingly.
 * <p>
 * <b>Block items:</b> every block the player may build with gets an item, which
 * is built through {@link Item.Builder#buildBlock(com.philia093.neofactory.block.Block)}.
 * Such an item borrows the picture and the colour of its block, which is what
 * makes a grey scale sheet such as {@code grass_top.png} look green in the
 * inventory as well. Air and water are skipped: air is not an item and water is
 * a fluid that the player never carries.
 * <p>
 * <b>Materials:</b> the items of a material - a plate of iron, a gear of copper, every shape a
 * metal comes in - are not written here. {@link Materials} builds them from the shapes the
 * material declares, which is what keeps this list short while the game holds hundreds of items;
 * {@link #registerAll()} names the point where they join the table. The two ingots that used to be
 * declared here moved there as well and kept their ids, see {@link #IRON_INGOT_ID}.
 * <p>
 * <b>Stack sizes:</b> materials, food and block items stack to
 * {@link Item#DEFAULT_MAX_STACK}, while tools are unique and stack to
 * {@link Item#SINGLE_ITEM_STACK}.
 * <p>
 * <b>Tools:</b> a tool names the kind of work it is good for - a pickaxe, an axe, a shovel, a hoe, a
 * sword - and the level of its material, see {@link ToolType}. Both are read by the mining rule:
 * the kind decides the speed and what a block hands over, the level decides whether it hands
 * anything over at all, see {@code HardnessMining}. A tool also names the amount of use it takes
 * before it is used up, which is not a property of a tool alone, see {@link Item#maxDamage()}.
 */
public final class Items {

    /** The empty item, it is never drawn and never carried by the player. */
    public static final int AIR_ID = 0;

    // ------------------------------------------------------------------
    // Block items. Keep the existing values unchanged and append new ones.
    // ------------------------------------------------------------------

    public static final int STONE_ID = 1;
    public static final int DIRT_ID = 2;
    public static final int GRASS_ID = 3;
    public static final int SAND_ID = 4;
    public static final int GRAVEL_ID = 5;
    public static final int CLAY_ID = 6;
    public static final int SANDSTONE_ID = 7;
    public static final int COAL_ORE_ID = 8;
    public static final int IRON_ORE_ID = 9;
    // The numbers 10 (snow) and 14 (tall grass) are free: those two blocks lost their art with the
    // flat engine, so they left the game, see Blocks. A new item still takes a number from
    // {@link #NEXT_FREE_ID} and never one of these.
    public static final int LOG_OAK_ID = 11;
    public static final int LEAVES_OAK_ID = 12;
    public static final int PLANKS_OAK_ID = 13;
    public static final int BEDROCK_ID = 15;

    // ------------------------------------------------------------------
    // Materials.
    // ------------------------------------------------------------------

    public static final int STICK_ID = 16;
    public static final int COAL_ID = 17;
    public static final int CHARCOAL_ID = 18;
    /**
     * Ids of the two ingots a material owns.
     * <p>
     * The items themselves are not written here any more: iron and gold became materials and
     * their ingots are the {@code INGOT} shape of them, registered by
     * {@link com.philia093.neofactory.material.Materials} with these very numbers. The ids stay in
     * this file because this is where the history of the item ids is written down - a stored
     * inventory spells {@code 19} out and means the iron ingot, no matter which class builds it.
     */
    public static final int IRON_INGOT_ID = 19;
    public static final int GOLD_INGOT_ID = 20;
    public static final int DIAMOND_ID = 21;
    public static final int EMERALD_ID = 22;
    public static final int REDSTONE_DUST_ID = 23;
    public static final int GLOWSTONE_DUST_ID = 24;
    public static final int CLAY_BALL_ID = 25;
    public static final int FLINT_ID = 26;
    public static final int FEATHER_ID = 27;
    public static final int LEATHER_ID = 28;
    public static final int BONE_ID = 29;
    public static final int STRING_ID = 30;
    public static final int PAPER_ID = 31;
    public static final int WHEAT_ID = 32;
    public static final int SEEDS_WHEAT_ID = 33;
    public static final int GUNPOWDER_ID = 34;
    public static final int BLAZE_ROD_ID = 35;
    public static final int BLAZE_POWDER_ID = 36;
    public static final int SUGAR_ID = 37;

    // ------------------------------------------------------------------
    // Food.
    // ------------------------------------------------------------------

    public static final int APPLE_ID = 38;
    public static final int BREAD_ID = 39;
    public static final int COOKIE_ID = 40;
    public static final int CARROT_ID = 41;
    public static final int POTATO_ID = 42;
    public static final int BAKED_POTATO_ID = 43;
    public static final int COOKED_BEEF_ID = 44;
    public static final int COOKED_PORKCHOP_ID = 45;
    public static final int COOKED_CHICKEN_ID = 46;
    public static final int MELON_ID = 47;

    // ------------------------------------------------------------------
    // Tools.
    // ------------------------------------------------------------------

    /**
     * Mining level of the iron tools.
     * <p>
     * Compared with {@link com.philia093.neofactory.block.Block#harvestLevel()},
     * see {@code HardnessMining}. The levels are {@code 2} for iron and {@code 3}
     * for diamond, a bare hand counts as {@code 0}.
     */
    public static final int IRON_TOOL_LEVEL = 2;

    /** Mining level of the diamond tools. */
    public static final int DIAMOND_TOOL_LEVEL = 3;

    /**
     * Mining level of a bare hand, also the level of a tool that opens no block.
     * <p>
     * A hoe and a sword carry it: they are tools of the hand and not of the stone, so they mine
     * every block a hand may mine and no block that asks for a level.
     */
    public static final int HAND_TOOL_LEVEL = 0;

    /** Speed iron tools break blocks with, a bare hand reaches {@code 1}. */
    public static final float IRON_TOOL_SPEED = 6.0f;

    /** Speed diamond tools break blocks with. */
    public static final float DIAMOND_TOOL_SPEED = 8.0f;

    /**
     * Amount of use a tool of iron takes before it is used up.
     * <p>
     * Beaten out of the original game: iron lasts {@code 250} blocks and diamond {@code 1561}, see
     * {@link Item#maxDamage()} and {@link com.philia093.neofactory.item.Damageable}.
     */
    public static final int IRON_TOOL_DURABILITY = 250;

    /** Amount of use a tool of diamond takes before it is used up. */
    public static final int DIAMOND_TOOL_DURABILITY = 1561;

    /**
     * Amount of use the wrench takes before it is used up.
     * <p>
     * A wrench is swung at a block as rarely as a hoe is: the work it is made for is the turn of a face,
     * which costs it nothing, so its life is the one of an iron tool and is only spent when a player
     * breaks a block with it in hand, see {@code GameScreen#wearHeldTool}.
     */
    public static final int WRENCH_DURABILITY = 250;

    public static final int IRON_PICKAXE_ID = 48;
    public static final int IRON_AXE_ID = 49;
    public static final int IRON_SHOVEL_ID = 50;
    public static final int IRON_HOE_ID = 51;
    public static final int IRON_SWORD_ID = 52;
    public static final int DIAMOND_PICKAXE_ID = 53;
    public static final int DIAMOND_AXE_ID = 54;
    public static final int DIAMOND_SHOVEL_ID = 55;
    public static final int DIAMOND_HOE_ID = 56;
    public static final int DIAMOND_SWORD_ID = 57;

    /**
     * Ids the armour of the game used to hold.
     * <p>
     * The eight pieces left the game for now: they were icons without slots and without a defence
     * behind them, so nothing spells these numbers any more. They are kept here as the history of
     * the id space - a stored inventory may still carry one and is read as an empty slot - and a new
     * item still takes a number from {@link #NEXT_FREE_ID}, never one of these.
     */
    public static final int ARMOUR_ID_FROM = 58;

    /** Last id the armour of the game used to hold, see {@link #ARMOUR_ID_FROM}. */
    public static final int ARMOUR_ID_TO = 65;

    public static final int FURNACE_ID = 66;

    /**
     * Ids of the cells, the containers of fluid.
     * <p>
     * Every one of them keeps its value forever: a stored inventory names them, see {@link FluidCells}.
     * <p>
     * <b>The numbers 67, 68 and 69 stay free.</b> They held the empty bucket, the bucket of water and
     * the bucket of lava, which left the game together with the fluids that used to stand in the world
     * as a block. Their place is not handed out again, so no stored inventory changes its meaning, the
     * same way the ids of the armour are kept free below.
     */
    public static final int FLUID_CELL_ID = 70;
    public static final int WATER_CELL_ID = 71;
    public static final int LAVA_CELL_ID = 72;

    /**
     * Id of the cell of steam, the first item of the industry.
     * <p>
     * It takes the first number no item held, see {@link #NEXT_FREE_ID}: the cells of the two fluids of
     * the world are the only items above the tools, and a fluid the industry brings is a fluid like any
     * other - it travels in a cell as well, see {@link FluidCells}.
     */
    public static final int STEAM_CELL_ID = 86;

    /**
     * Id of the bronze boiler, the block that a player builds to make steam.
     * <p>
     * It comes right behind the cell of steam, and the machines that follow take the numbers after it -
     * see {@link #NEXT_FREE_ID} for why they are counted together with the materials behind them.
     */
    public static final int BRONZE_BOILER_ID = 87;

    /**
     * Id of the wrench, the tool the industry is built with.
     * <p>
     * It takes a number that the window between the boiler and the materials held free, see
     * {@link #PIPE_ID_FROM}: the wrench is not a thing of a material and needs no room of its own, and a
     * number that no stored inventory can hold costs no version of the save game, see
     * {@link #NEXT_FREE_ID}.
     */
    public static final int WRENCH_ID = 88;

    /**
     * Id of the first pipe, one item per material and size.
     * <p>
     * The twenty eight pipes do not fit into the numbers between the boiler and the materials, so they take
     * a run of their own and the materials stand behind it, see {@link #NEXT_FREE_ID}. {@code Pipes} hands
     * the numbers out in the order its materials and sizes are declared.
     */
    public static final int PIPE_ID_FROM = 100;

    // ------------------------------------------------------------------
    // The block items of the blocks that came with the third axis. They are appended above every
    // number that was written down before them, so no id that a world already holds changes its
    // meaning, see the class comment.
    // ------------------------------------------------------------------

    public static final int COBBLESTONE_ID = 73;
    public static final int STONE_BRICK_ID = 74;
    public static final int STONE_SLAB_ID = 75;
    public static final int BRICK_ID = 76;
    public static final int GLASS_ID = 77;
    public static final int GLOWSTONE_ID = 78;
    public static final int OBSIDIAN_ID = 79;
    public static final int SAPLING_OAK_ID = 80;
    public static final int CRAFTING_TABLE_ID = 81;
    public static final int ANVIL_ID = 82;
    public static final int CAULDRON_ID = 83;
    public static final int TORCH_ID = 84;
    public static final int LADDER_ID = 85;

    /**
     * Next unused item id, used to verify that a new item got a fresh id.
     * <p>
     * The materials hand their ids out from this number on, see
     * {@link Materials#registerAll()}: a hand written item that is appended above it takes the
     * number itself and bumps this one, while the run of the materials follows behind. A number
     * from the middle may never be taken, or the items of the materials would move.
     * <p>
     * <b>The industry writes its items down before the materials.</b> The cell of steam took 86, the
     * bronze boiler 87 and the twenty eight pipes 100 to 127, and the machines and tools of the coming
     * steps take the numbers behind them; this number is therefore set past that run, so the materials
     * keep their place while the industry grows. One of those items costs a line here and moves nothing at
     * all.
     * <p>
     * <b>What version 8 of the save game changed.</b> The items of the industry took the numbers 86 to 99,
     * so every item of every material stands fourteen numbers higher than it did - which is why a world of
     * an older version is refused instead of being read with the wrong items in it, see
     * {@link com.philia093.neofactory.world.save.SaveFormat#DATA_VERSION}.
     * <p>
     * <b>What version 10 changed.</b> The pipes took the numbers 100 to 127 - fourteen items more than the
     * window the industry had kept free - and bronze and steel joined the materials, so every item of every
     * material stands another twenty eight numbers higher. A stored world of version 9 is refused for the
     * same reason as one of version 7.
     */
    public static final int NEXT_FREE_ID = 128;

    /**
     * Amount an empty container stacks to.
     * <p>
     * An empty cell is a tool the player carries around, so it stacks like every other material of the
     * game.
     */
    private static final int EMPTY_CONTAINER_STACK = Item.DEFAULT_MAX_STACK;

    /**
     * Amount a container with a fluid in it stacks to.
     * <p>
     * A full cell is one cell and one fluid, so the fluids of the game do not stack - a stack of them
     * would hold several kinds at once. A machine therefore takes one full cell out of a slot and hands
     * an empty one back, see {@link FluidContainer}.
     */
    private static final int FULL_CONTAINER_STACK = Item.SINGLE_ITEM_STACK;

    // ------------------------------------------------------------------
    // Item instances. They are filled by registerAll().
    // ------------------------------------------------------------------

    /** The empty item, it is never drawn and never stacked. */
    public static Item AIR;

    /** Stone block item. */
    public static Item STONE;
    /** Dirt block item. */
    public static Item DIRT;
    /** Grass block item. */
    public static Item GRASS;
    /** Sand block item. */
    public static Item SAND;
    /** Gravel block item. */
    public static Item GRAVEL;
    /** Clay block item. */
    public static Item CLAY;
    /** Sandstone block item. */
    public static Item SANDSTONE;
    /** Coal ore block item. */
    public static Item COAL_ORE;
    /** Iron ore block item. */
    public static Item IRON_ORE;
    /** Oak log block item. */
    public static Item LOG_OAK;
    /** Oak leaves block item. */
    public static Item LEAVES_OAK;
    /** Oak planks block item. */
    public static Item PLANKS_OAK;

    /** The furnace, the block a player builds to smelt with. */
    public static Item FURNACE;
    /** Bedrock block item, unbreakable in the world. */
    public static Item BEDROCK;

    /** Cobblestone block item. */
    public static Item COBBLESTONE;
    /** Stone brick block item. */
    public static Item STONE_BRICK;
    /** Stone slab block item, half a block high. */
    public static Item STONE_SLAB;
    /** Brick block item. */
    public static Item BRICK;
    /** Glass block item. */
    public static Item GLASS;
    /** Glowstone block item. */
    public static Item GLOWSTONE;
    /** Obsidian block item. */
    public static Item OBSIDIAN;
    /** Oak sapling block item. */
    public static Item SAPLING_OAK;
    /** Crafting table block item. */
    public static Item CRAFTING_TABLE;
    /** Anvil block item. */
    public static Item ANVIL;
    /** Cauldron block item. */
    public static Item CAULDRON;
    /** Torch block item. */
    public static Item TORCH;
    /** Ladder block item. */
    public static Item LADDER;

    /** Stick, the simplest building material. */
    public static Item STICK;
    /** Coal, dug out of coal ore. */
    public static Item COAL;
    /** Charcoal, the result of burning wood. */
    public static Item CHARCOAL;
    // The ingots of iron and gold are not declared here any more: they are the INGOT shape of the
    // materials of those two names, see Materials.
    /** Diamond, the hardest material of the game. */
    public static Item DIAMOND;
    /** Emerald, a rare gem. */
    public static Item EMERALD;
    /** Redstone dust, used for mechanisms. */
    public static Item REDSTONE_DUST;
    /** Glowstone dust, a bright material. */
    public static Item GLOWSTONE_DUST;
    /** Clay ball, dug out of clay. */
    public static Item CLAY_BALL;
    /** Flint, found next to gravel. */
    public static Item FLINT;
    /** Feather, dropped by chickens. */
    public static Item FEATHER;
    /** Leather, dropped by cows. */
    public static Item LEATHER;
    /** Bone, dropped by skeletons. */
    public static Item BONE;
    /** String, dropped by spiders. */
    public static Item STRING;
    /** Paper, made from sugar cane. */
    public static Item PAPER;
    /** Wheat, harvested on farmland. */
    public static Item WHEAT;
    /** Wheat seeds, planted on farmland. */
    public static Item SEEDS_WHEAT;
    /** Gunpowder, dropped by creepers. */
    public static Item GUNPOWDER;
    /** Blaze rod, dropped in the nether. */
    public static Item BLAZE_ROD;
    /** Blaze powder, ground from a blaze rod. */
    public static Item BLAZE_POWDER;
    /** Sugar, made from sugar cane. */
    public static Item SUGAR;

    /** Apple, the classic food. */
    public static Item APPLE;
    /** Bread, baked from wheat. */
    public static Item BREAD;
    /** Cookie, a small snack. */
    public static Item COOKIE;
    /** Carrot, a vegetable. */
    public static Item CARROT;
    /** Potato, a raw vegetable. */
    public static Item POTATO;
    /** Baked potato, a cooked vegetable. */
    public static Item BAKED_POTATO;
    /** Steak, cooked beef. */
    public static Item COOKED_BEEF;
    /** Cooked porkchop. */
    public static Item COOKED_PORKCHOP;
    /** Cooked chicken. */
    public static Item COOKED_CHICKEN;
    /** Melon slice. */
    public static Item MELON;

    /** Iron pickaxe. */
    public static Item IRON_PICKAXE;
    /** Iron axe. */
    public static Item IRON_AXE;
    /** Iron shovel. */
    public static Item IRON_SHOVEL;
    /** Iron hoe. */
    public static Item IRON_HOE;
    /** Iron sword. */
    public static Item IRON_SWORD;
    /** The wrench, the tool that opens a side of a pipe and turns what stands built. */
    public static Item WRENCH;
    /** Diamond pickaxe. */
    public static Item DIAMOND_PICKAXE;
    /** Diamond axe. */
    public static Item DIAMOND_AXE;
    /** Diamond shovel. */
    public static Item DIAMOND_SHOVEL;
    /** Diamond hoe. */
    public static Item DIAMOND_HOE;
    /** Diamond sword. */
    public static Item DIAMOND_SWORD;

    /** The empty cell, the container of the industry that takes any fluid. */
    public static Item FLUID_CELL;

    /** A cell of water. */
    public static Item WATER_CELL;

    /** A cell of lava. */
    public static Item LAVA_CELL;

    /** A cell of steam, the fluid the first machines of the industry run on. */
    public static Item STEAM_CELL;

    /** The bronze boiler, the block a player builds to make steam. */
    public static Item BRONZE_BOILER;

    /**
     * Creates and registers every item.
     * <p>
     * Called once during startup, before any inventory exists. The block registry
     * has to be filled first, because a block item looks up its block while it is
     * built.
     */
    public static void registerAll() {
        AIR = register(Item.builder(AIR_ID, "air")
                .displayName("Air")
                .maxStackSize(Item.SINGLE_ITEM_STACK)
                .build());

        // Block items borrow the picture and the colour of their block, so the
        // inventory shows exactly the tile the player sees in the world.
        STONE = register(Item.builder(STONE_ID, "stone")
                .displayName("Stone")
                .buildBlock(Blocks.STONE));
        DIRT = register(Item.builder(DIRT_ID, "dirt")
                .displayName("Dirt")
                .buildBlock(Blocks.DIRT));
        GRASS = register(Item.builder(GRASS_ID, "grass")
                .displayName("Grass Block")
                .buildBlock(Blocks.GRASS));
        SAND = register(Item.builder(SAND_ID, "sand")
                .displayName("Sand")
                .buildBlock(Blocks.SAND));
        GRAVEL = register(Item.builder(GRAVEL_ID, "gravel")
                .displayName("Gravel")
                .buildBlock(Blocks.GRAVEL));
        CLAY = register(Item.builder(CLAY_ID, "clay")
                .displayName("Clay")
                .buildBlock(Blocks.CLAY));
        SANDSTONE = register(Item.builder(SANDSTONE_ID, "sandstone")
                .displayName("Sandstone")
                .buildBlock(Blocks.SANDSTONE));
        COAL_ORE = register(Item.builder(COAL_ORE_ID, "coal_ore")
                .displayName("Coal Ore")
                .buildBlock(Blocks.COAL_ORE));
        IRON_ORE = register(Item.builder(IRON_ORE_ID, "iron_ore")
                .displayName("Iron Ore")
                .buildBlock(Blocks.IRON_ORE));
        LOG_OAK = register(Item.builder(LOG_OAK_ID, "log_oak")
                .displayName("Oak Log")
                .buildBlock(Blocks.LOG_OAK));
        LEAVES_OAK = register(Item.builder(LEAVES_OAK_ID, "leaves_oak")
                .displayName("Oak Leaves")
                .buildBlock(Blocks.LEAVES_OAK));
        PLANKS_OAK = register(Item.builder(PLANKS_OAK_ID, "planks_oak")
                .displayName("Oak Planks")
                .buildBlock(Blocks.PLANKS_OAK));
        // Tall grass lost its picture with the flat engine and left the game together with its
        // block, see Blocks. The place its item held stays empty, which is what keeps every id
        // below the materials where it was.
        FURNACE = register(Item.builder(FURNACE_ID, "furnace")
                .displayName("Furnace")
                .buildBlock(Blocks.FURNACE));
        BEDROCK = register(Item.builder(BEDROCK_ID, "bedrock")
                .displayName("Bedrock")
                .buildBlock(Blocks.BEDROCK));

        // The block items of the third axis: cobblestone, the shop of a builder and the plants of a
        // meadow. They borrow their picture from the block they place, like every block item above.
        COBBLESTONE = register(Item.builder(COBBLESTONE_ID, "cobblestone")
                .displayName("Cobblestone")
                .buildBlock(Blocks.COBBLESTONE));
        STONE_BRICK = register(Item.builder(STONE_BRICK_ID, "stonebrick")
                .displayName("Stone Bricks")
                .buildBlock(Blocks.STONE_BRICK));
        STONE_SLAB = register(Item.builder(STONE_SLAB_ID, "stone_slab")
                .displayName("Stone Slab")
                .buildBlock(Blocks.STONE_SLAB));
        BRICK = register(Item.builder(BRICK_ID, "brick")
                .displayName("Bricks")
                .buildBlock(Blocks.BRICK));
        GLASS = register(Item.builder(GLASS_ID, "glass")
                .displayName("Glass")
                .buildBlock(Blocks.GLASS));
        GLOWSTONE = register(Item.builder(GLOWSTONE_ID, "glowstone")
                .displayName("Glowstone")
                .buildBlock(Blocks.GLOWSTONE));
        OBSIDIAN = register(Item.builder(OBSIDIAN_ID, "obsidian")
                .displayName("Obsidian")
                .buildBlock(Blocks.OBSIDIAN));
        SAPLING_OAK = register(Item.builder(SAPLING_OAK_ID, "sapling_oak")
                .displayName("Oak Sapling")
                .buildBlock(Blocks.SAPLING_OAK));
        CRAFTING_TABLE = register(Item.builder(CRAFTING_TABLE_ID, "crafting_table")
                .displayName("Crafting Table")
                .buildBlock(Blocks.CRAFTING_TABLE));
        ANVIL = register(Item.builder(ANVIL_ID, "anvil")
                .displayName("Anvil")
                .buildBlock(Blocks.ANVIL));
        CAULDRON = register(Item.builder(CAULDRON_ID, "cauldron")
                .displayName("Cauldron")
                .buildBlock(Blocks.CAULDRON));
        TORCH = register(Item.builder(TORCH_ID, "torch")
                .displayName("Torch")
                .buildBlock(Blocks.TORCH));
        LADDER = register(Item.builder(LADDER_ID, "ladder")
                .displayName("Ladder")
                .buildBlock(Blocks.LADDER));

        // Materials. The icon of a plain item names its folder, because a bare
        // texture name is resolved inside the block folder.
        STICK = register(Item.builder(STICK_ID, "stick")
                .displayName("Stick")
                .texture(Item.ITEM_FOLDER + "stick")
                .build());
        COAL = register(Item.builder(COAL_ID, "coal")
                .displayName("Coal")
                .texture(Item.ITEM_FOLDER + "coal")
                .build());
        CHARCOAL = register(Item.builder(CHARCOAL_ID, "charcoal")
                .displayName("Charcoal")
                .texture(Item.ITEM_FOLDER + "charcoal")
                .build());
        // The two metals of the game do not spell their ingot out here: iron and gold are
        // materials, and an ingot of them is the INGOT shape of that material. Materials registers
        // them with the very ids IRON_INGOT_ID and GOLD_INGOT_ID, see Materials.
        DIAMOND = register(Item.builder(DIAMOND_ID, "diamond")
                .displayName("Diamond")
                .texture(Item.ITEM_FOLDER + "diamond")
                .build());
        EMERALD = register(Item.builder(EMERALD_ID, "emerald")
                .displayName("Emerald")
                .texture(Item.ITEM_FOLDER + "emerald")
                .build());
        REDSTONE_DUST = register(Item.builder(REDSTONE_DUST_ID, "redstone_dust")
                .displayName("Redstone Dust")
                .texture(Item.ITEM_FOLDER + "redstone_dust")
                .build());
        GLOWSTONE_DUST = register(Item.builder(GLOWSTONE_DUST_ID, "glowstone_dust")
                .displayName("Glowstone Dust")
                .texture(Item.ITEM_FOLDER + "glowstone_dust")
                .build());
        CLAY_BALL = register(Item.builder(CLAY_BALL_ID, "clay_ball")
                .displayName("Clay Ball")
                .texture(Item.ITEM_FOLDER + "clay_ball")
                .build());
        FLINT = register(Item.builder(FLINT_ID, "flint")
                .displayName("Flint")
                .texture(Item.ITEM_FOLDER + "flint")
                .build());
        FEATHER = register(Item.builder(FEATHER_ID, "feather")
                .displayName("Feather")
                .texture(Item.ITEM_FOLDER + "feather")
                .build());
        LEATHER = register(Item.builder(LEATHER_ID, "leather")
                .displayName("Leather")
                .texture(Item.ITEM_FOLDER + "leather")
                .build());
        BONE = register(Item.builder(BONE_ID, "bone")
                .displayName("Bone")
                .texture(Item.ITEM_FOLDER + "bone")
                .build());
        STRING = register(Item.builder(STRING_ID, "string")
                .displayName("String")
                .texture(Item.ITEM_FOLDER + "string")
                .build());
        PAPER = register(Item.builder(PAPER_ID, "paper")
                .displayName("Paper")
                .texture(Item.ITEM_FOLDER + "paper")
                .build());
        WHEAT = register(Item.builder(WHEAT_ID, "wheat")
                .displayName("Wheat")
                .texture(Item.ITEM_FOLDER + "wheat")
                .build());
        SEEDS_WHEAT = register(Item.builder(SEEDS_WHEAT_ID, "seeds_wheat")
                .displayName("Wheat Seeds")
                .texture(Item.ITEM_FOLDER + "seeds_wheat")
                .build());
        GUNPOWDER = register(Item.builder(GUNPOWDER_ID, "gunpowder")
                .displayName("Gunpowder")
                .texture(Item.ITEM_FOLDER + "gunpowder")
                .build());
        BLAZE_ROD = register(Item.builder(BLAZE_ROD_ID, "blaze_rod")
                .displayName("Blaze Rod")
                .texture(Item.ITEM_FOLDER + "blaze_rod")
                .build());
        BLAZE_POWDER = register(Item.builder(BLAZE_POWDER_ID, "blaze_powder")
                .displayName("Blaze Powder")
                .texture(Item.ITEM_FOLDER + "blaze_powder")
                .build());
        SUGAR = register(Item.builder(SUGAR_ID, "sugar")
                .displayName("Sugar")
                .texture(Item.ITEM_FOLDER + "sugar")
                .build());

        // Food.
        APPLE = register(Item.builder(APPLE_ID, "apple")
                .displayName("Apple")
                .texture(Item.ITEM_FOLDER + "apple")
                .build());
        BREAD = register(Item.builder(BREAD_ID, "bread")
                .displayName("Bread")
                .texture(Item.ITEM_FOLDER + "bread")
                .build());
        COOKIE = register(Item.builder(COOKIE_ID, "cookie")
                .displayName("Cookie")
                .texture(Item.ITEM_FOLDER + "cookie")
                .build());
        CARROT = register(Item.builder(CARROT_ID, "carrot")
                .displayName("Carrot")
                .texture(Item.ITEM_FOLDER + "carrot")
                .build());
        POTATO = register(Item.builder(POTATO_ID, "potato")
                .displayName("Potato")
                .texture(Item.ITEM_FOLDER + "potato")
                .build());
        BAKED_POTATO = register(Item.builder(BAKED_POTATO_ID, "baked_potato")
                .displayName("Baked Potato")
                .texture(Item.ITEM_FOLDER + "potato_baked")
                .build());
        COOKED_BEEF = register(Item.builder(COOKED_BEEF_ID, "cooked_beef")
                .displayName("Steak")
                .texture(Item.ITEM_FOLDER + "beef_cooked")
                .build());
        COOKED_PORKCHOP = register(Item.builder(COOKED_PORKCHOP_ID, "cooked_porkchop")
                .displayName("Cooked Porkchop")
                .texture(Item.ITEM_FOLDER + "porkchop_cooked")
                .build());
        COOKED_CHICKEN = register(Item.builder(COOKED_CHICKEN_ID, "cooked_chicken")
                .displayName("Cooked Chicken")
                .texture(Item.ITEM_FOLDER + "chicken_cooked")
                .build());
        MELON = register(Item.builder(MELON_ID, "melon")
                .displayName("Melon Slice")
                .texture(Item.ITEM_FOLDER + "melon")
                .build());

        // Tools. They are unique, so a full stack holds a single piece. Every tool names the kind of
        // work it is good for and the level of its material, which the mining rule reads, see ToolType
        // and HardnessMining; what it wears is the life of its own, see Item#maxDamage. A hoe and a
        // sword carry the level of a hand and the speed of one: they open no block of the game, and a
        // sword cuts what stands instead of what lies.
        IRON_PICKAXE = register(toolItem(IRON_PICKAXE_ID, "iron_pickaxe", "Iron Pickaxe",
                ToolType.PICKAXE, IRON_TOOL_LEVEL, IRON_TOOL_SPEED, IRON_TOOL_DURABILITY));
        IRON_AXE = register(toolItem(IRON_AXE_ID, "iron_axe", "Iron Axe",
                ToolType.AXE, IRON_TOOL_LEVEL, IRON_TOOL_SPEED, IRON_TOOL_DURABILITY));
        IRON_SHOVEL = register(toolItem(IRON_SHOVEL_ID, "iron_shovel", "Iron Shovel",
                ToolType.SHOVEL, IRON_TOOL_LEVEL, IRON_TOOL_SPEED, IRON_TOOL_DURABILITY));
        IRON_HOE = register(toolItem(IRON_HOE_ID, "iron_hoe", "Iron Hoe",
                ToolType.HOE, HAND_TOOL_LEVEL, Item.HAND_MINING_SPEED, IRON_TOOL_DURABILITY));
        IRON_SWORD = register(toolItem(IRON_SWORD_ID, "iron_sword", "Iron Sword",
                ToolType.SWORD, HAND_TOOL_LEVEL, Item.HAND_MINING_SPEED, IRON_TOOL_DURABILITY));
        DIAMOND_PICKAXE = register(toolItem(DIAMOND_PICKAXE_ID, "diamond_pickaxe", "Diamond Pickaxe",
                ToolType.PICKAXE, DIAMOND_TOOL_LEVEL, DIAMOND_TOOL_SPEED, DIAMOND_TOOL_DURABILITY));
        DIAMOND_AXE = register(toolItem(DIAMOND_AXE_ID, "diamond_axe", "Diamond Axe",
                ToolType.AXE, DIAMOND_TOOL_LEVEL, DIAMOND_TOOL_SPEED, DIAMOND_TOOL_DURABILITY));
        DIAMOND_SHOVEL = register(toolItem(DIAMOND_SHOVEL_ID, "diamond_shovel", "Diamond Shovel",
                ToolType.SHOVEL, DIAMOND_TOOL_LEVEL, DIAMOND_TOOL_SPEED, DIAMOND_TOOL_DURABILITY));
        DIAMOND_HOE = register(toolItem(DIAMOND_HOE_ID, "diamond_hoe", "Diamond Hoe",
                ToolType.HOE, HAND_TOOL_LEVEL, Item.HAND_MINING_SPEED, DIAMOND_TOOL_DURABILITY));
        DIAMOND_SWORD = register(toolItem(DIAMOND_SWORD_ID, "diamond_sword", "Diamond Sword",
                ToolType.SWORD, HAND_TOOL_LEVEL, Item.HAND_MINING_SPEED, DIAMOND_TOOL_DURABILITY));

        // The wrench: the tool a player builds the industry with. It opens no block, it opens faces - a
        // side of a pipe, the front of a machine - so it is built here and not by toolItem(): its kind is
        // the one it turns blocks with, its level is the one of a hand, and FaceTool#WRENCH is what makes
        // the grid of nine cells appear on the block a player looks at, see Item#faceTool.
        WRENCH = register(Item.builder(WRENCH_ID, "wrench")
                .displayName("Wrench")
                .texture(Item.ITEM_FOLDER + "wrench")
                .maxStackSize(Item.SINGLE_ITEM_STACK)
                .toolType(ToolType.WRENCH)
                .toolLevel(HAND_TOOL_LEVEL)
                .miningSpeed(Item.HAND_MINING_SPEED)
                .maxDamage(WRENCH_DURABILITY)
                .faceTool(FaceTool.WRENCH)
                .build());

        // The armour is gone for now: eight icons that no slot held and that no defence stood behind.
        // Its numbers stay free, see ARMOUR_ID_FROM above.

        // The fluids of the game travel in cells, see FluidContainer: one body of steel for every fluid,
        // drawn from the one grey scale picture of the game and painted in the colour of whatever is
        // inside, so a new fluid needs a line here and no art. The game has no bucket any more - the
        // buckets belonged to the fluids that stood in the world as a block.
        FLUID_CELL = register(Item.builder(FLUID_CELL_ID, "fluid_cell")
                .displayName("Fluid Cell")
                .texture(Item.ITEM_FOLDER + "fluid_cell")
                .maxStackSize(EMPTY_CONTAINER_STACK)
                .container(FluidContainer.cell())
                .build());
        WATER_CELL = register(fluidCell(WATER_CELL_ID, "water_cell", "Water Cell", Fluids.WATER));
        LAVA_CELL = register(fluidCell(LAVA_CELL_ID, "lava_cell", "Lava Cell", Fluids.LAVA));
        STEAM_CELL = register(fluidCell(STEAM_CELL_ID, "steam_cell", "Steam Cell", Fluids.STEAM));

        // The machines of the industry. A machine is a block item like every other one: it borrows the
        // picture of its block, and the block names the entity that holds its slots and tanks.
        BRONZE_BOILER = register(Item.builder(BRONZE_BOILER_ID, "bronze_boiler")
                .displayName("Bronze Boiler")
                .buildBlock(Blocks.BRONZE_BOILER));

        // The pipes of the industry: one item per material and size, the blocks of them were built while
        // the blocks were registered, see Pipes. They take the numbers 100 to 127, a run of their own:
        // the wrench is the only item that stands in the window between the boiler and them.
        Pipes.registerItems();

        // The materials bring their own items, one per shape they come in. They are registered
        // here, right before the table closes, so that they append behind every item written above
        // and none of those ids moves, see Materials.
        Materials.registerAll();

        ItemRegistry.freeze();
    }

    /**
     * Builds a cell that carries a fluid.
     * <p>
     * The picture is the grey scale sheet of every cell, the colour comes from the fluid, see
     * {@link Fluid#color()}.
     *
     * @param id numeric item id
     * @param name technical name of the item
     * @param displayName name shown to the player
     * @param fluid fluid the cell carries
     * @return the item definition
     */
    private static Item fluidCell(int id, String name, String displayName, Fluid fluid) {
        // The colour of the fluid is painted into the window of the cell and not multiplied over
        // the whole item, see CellIconFactory: the steel of the container stays grey.
        return Item.builder(id, name)
                .displayName(displayName)
                .texture(Item.ITEM_FOLDER + "fluid_cell")
                .maxStackSize(FULL_CONTAINER_STACK)
                .container(FluidContainer.cell(fluid))
                .build();
    }

    /**
     * Registers an item and returns it.
     * <p>
     * The helper keeps a definition down to a single statement, which makes the
     * long list above readable.
     *
     * @param item item to register
     * @return the registered item
     */
    private static Item register(Item item) {
        ItemRegistry.register(item);
        return item;
    }

    /**
     * Builds a tool that breaks blocks.
     * <p>
     * A tool is unique like every other one. It carries three things the mining rule reads: the kind
     * of work it is good for, the mining level of its material and the speed it breaks a block of
     * that kind with, see {@link ToolType} and {@code HardnessMining}. The life it takes is what
     * makes it wear with every block it harvests, see {@link Item#maxDamage()}.
     *
     * @param id numeric item id
     * @param name technical name, also the name of the icon
     * @param displayName name shown to the player
     * @param toolType kind of work this tool is good for
     * @param toolLevel mining level of the tool
     * @param miningSpeed speed the tool breaks blocks with
     * @param maxDamage amount of use the tool takes before it is used up
     * @return the item definition, stacking to a single piece
     */
    private static Item toolItem(int id, String name, String displayName, ToolType toolType,
            int toolLevel, float miningSpeed, int maxDamage) {
        return Item.builder(id, name)
                .displayName(displayName)
                .texture(Item.ITEM_FOLDER + name)
                .maxStackSize(Item.SINGLE_ITEM_STACK)
                .toolType(toolType)
                .toolLevel(toolLevel)
                .miningSpeed(miningSpeed)
                .maxDamage(maxDamage)
                .build();
    }
}






