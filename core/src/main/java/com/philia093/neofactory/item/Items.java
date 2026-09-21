package com.philia093.neofactory.item;

import com.philia093.neofactory.block.Blocks;

/**
 * Declaration of every item type used by the game.
 * <p>
 * The list is split into four groups: the items that place a block, the raw
 * materials, the food and the unique items such as tools and armour. Ids are
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
 * <b>Stack sizes:</b> materials, food and block items stack to
 * {@link Item#DEFAULT_MAX_STACK}, while tools and armour are unique and stack to
 * {@link Item#SINGLE_ITEM_STACK}.
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
    public static final int SNOW_ID = 10;
    public static final int LOG_OAK_ID = 11;
    public static final int LEAVES_OAK_ID = 12;
    public static final int PLANKS_OAK_ID = 13;
    public static final int TALL_GRASS_ID = 14;
    public static final int BEDROCK_ID = 15;

    // ------------------------------------------------------------------
    // Materials.
    // ------------------------------------------------------------------

    public static final int STICK_ID = 16;
    public static final int COAL_ID = 17;
    public static final int CHARCOAL_ID = 18;
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

    /** Speed iron tools break blocks with, a bare hand reaches {@code 1}. */
    public static final float IRON_TOOL_SPEED = 6.0f;

    /** Speed diamond tools break blocks with. */
    public static final float DIAMOND_TOOL_SPEED = 8.0f;

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

    // ------------------------------------------------------------------
    // Armour.
    // ------------------------------------------------------------------

    public static final int IRON_HELMET_ID = 58;
    public static final int IRON_CHESTPLATE_ID = 59;
    public static final int IRON_LEGGINGS_ID = 60;
    public static final int IRON_BOOTS_ID = 61;
    public static final int DIAMOND_HELMET_ID = 62;
    public static final int DIAMOND_CHESTPLATE_ID = 63;
    public static final int DIAMOND_LEGGINGS_ID = 64;
    public static final int DIAMOND_BOOTS_ID = 65;
    public static final int FURNACE_ID = 66;

    /** Next unused item id, used to verify that a new item got a fresh id. */
    public static final int NEXT_FREE_ID = 67;

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
    /** Snow block item. */
    public static Item SNOW;
    /** Oak log block item. */
    public static Item LOG_OAK;
    /** Oak leaves block item. */
    public static Item LEAVES_OAK;
    /** Oak planks block item. */
    public static Item PLANKS_OAK;
    /** Tall grass block item. */
    public static Item TALL_GRASS;

    /** The furnace, the block a player builds to smelt with. */
    public static Item FURNACE;
    /** Bedrock block item, unbreakable in the world. */
    public static Item BEDROCK;

    /** Stick, the simplest building material. */
    public static Item STICK;
    /** Coal, dug out of coal ore. */
    public static Item COAL;
    /** Charcoal, the result of burning wood. */
    public static Item CHARCOAL;
    /** Iron ingot, smelted from iron ore. */
    public static Item IRON_INGOT;
    /** Gold ingot, smelted from gold ore. */
    public static Item GOLD_INGOT;
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

    /** Iron helmet. */
    public static Item IRON_HELMET;
    /** Iron chestplate. */
    public static Item IRON_CHESTPLATE;
    /** Iron leggings. */
    public static Item IRON_LEGGINGS;
    /** Iron boots. */
    public static Item IRON_BOOTS;
    /** Diamond helmet. */
    public static Item DIAMOND_HELMET;
    /** Diamond chestplate. */
    public static Item DIAMOND_CHESTPLATE;
    /** Diamond leggings. */
    public static Item DIAMOND_LEGGINGS;
    /** Diamond boots. */
    public static Item DIAMOND_BOOTS;

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
        SNOW = register(Item.builder(SNOW_ID, "snow")
                .displayName("Snow")
                .buildBlock(Blocks.SNOW));
        LOG_OAK = register(Item.builder(LOG_OAK_ID, "log_oak")
                .displayName("Oak Log")
                .buildBlock(Blocks.LOG_OAK));
        LEAVES_OAK = register(Item.builder(LEAVES_OAK_ID, "leaves_oak")
                .displayName("Oak Leaves")
                .buildBlock(Blocks.LEAVES_OAK));
        PLANKS_OAK = register(Item.builder(PLANKS_OAK_ID, "planks_oak")
                .displayName("Oak Planks")
                .buildBlock(Blocks.PLANKS_OAK));
        TALL_GRASS = register(Item.builder(TALL_GRASS_ID, "tall_grass")
                .displayName("Tall Grass")
                .buildBlock(Blocks.TALL_GRASS));
        FURNACE = register(Item.builder(FURNACE_ID, "furnace")
                .displayName("Furnace")
                .buildBlock(Blocks.FURNACE));
        BEDROCK = register(Item.builder(BEDROCK_ID, "bedrock")
                .displayName("Bedrock")
                .buildBlock(Blocks.BEDROCK));

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
        IRON_INGOT = register(Item.builder(IRON_INGOT_ID, "iron_ingot")
                .displayName("Iron Ingot")
                .texture(Item.ITEM_FOLDER + "iron_ingot")
                .build());
        GOLD_INGOT = register(Item.builder(GOLD_INGOT_ID, "gold_ingot")
                .displayName("Gold Ingot")
                .texture(Item.ITEM_FOLDER + "gold_ingot")
                .build());
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

        // Tools. They are unique, so a full stack holds a single piece. The mining
        // tools know their level and their speed, which a rule with break times
        // reads, see HardnessMining. Swords and hoes carry neither, because the tool
        // type that decides whether a tool fits a block is not modelled yet.
        IRON_PICKAXE = register(toolItem(IRON_PICKAXE_ID, "iron_pickaxe", "Iron Pickaxe",
                IRON_TOOL_LEVEL, IRON_TOOL_SPEED));
        IRON_AXE = register(toolItem(IRON_AXE_ID, "iron_axe", "Iron Axe",
                IRON_TOOL_LEVEL, IRON_TOOL_SPEED));
        IRON_SHOVEL = register(toolItem(IRON_SHOVEL_ID, "iron_shovel", "Iron Shovel",
                IRON_TOOL_LEVEL, IRON_TOOL_SPEED));
        IRON_HOE = register(uniqueItem(IRON_HOE_ID, "iron_hoe", "Iron Hoe"));
        IRON_SWORD = register(uniqueItem(IRON_SWORD_ID, "iron_sword", "Iron Sword"));
        DIAMOND_PICKAXE = register(toolItem(DIAMOND_PICKAXE_ID, "diamond_pickaxe", "Diamond Pickaxe",
                DIAMOND_TOOL_LEVEL, DIAMOND_TOOL_SPEED));
        DIAMOND_AXE = register(toolItem(DIAMOND_AXE_ID, "diamond_axe", "Diamond Axe",
                DIAMOND_TOOL_LEVEL, DIAMOND_TOOL_SPEED));
        DIAMOND_SHOVEL = register(toolItem(DIAMOND_SHOVEL_ID, "diamond_shovel", "Diamond Shovel",
                DIAMOND_TOOL_LEVEL, DIAMOND_TOOL_SPEED));
        DIAMOND_HOE = register(uniqueItem(DIAMOND_HOE_ID, "diamond_hoe", "Diamond Hoe"));
        DIAMOND_SWORD = register(uniqueItem(DIAMOND_SWORD_ID, "diamond_sword", "Diamond Sword"));

        // Armour. Only the icons exist so far: the slots of the inventory screen
        // are drawn but store nothing yet.
        IRON_HELMET = register(uniqueItem(IRON_HELMET_ID, "iron_helmet", "Iron Helmet"));
        IRON_CHESTPLATE = register(uniqueItem(IRON_CHESTPLATE_ID, "iron_chestplate", "Iron Chestplate"));
        IRON_LEGGINGS = register(uniqueItem(IRON_LEGGINGS_ID, "iron_leggings", "Iron Leggings"));
        IRON_BOOTS = register(uniqueItem(IRON_BOOTS_ID, "iron_boots", "Iron Boots"));
        DIAMOND_HELMET = register(uniqueItem(DIAMOND_HELMET_ID, "diamond_helmet", "Diamond Helmet"));
        DIAMOND_CHESTPLATE = register(uniqueItem(DIAMOND_CHESTPLATE_ID, "diamond_chestplate", "Diamond Chestplate"));
        DIAMOND_LEGGINGS = register(uniqueItem(DIAMOND_LEGGINGS_ID, "diamond_leggings", "Diamond Leggings"));
        DIAMOND_BOOTS = register(uniqueItem(DIAMOND_BOOTS_ID, "diamond_boots", "Diamond Boots"));

        ItemRegistry.freeze();
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
     * Builds an item the player only owns once, such as a tool or a piece of
     * armour.
     *
     * @param id numeric item id
     * @param name technical name, also the name of the icon
     * @param displayName name shown to the player
     * @return the item definition, stacking to a single piece
     */
    private static Item uniqueItem(int id, String name, String displayName) {
        return Item.builder(id, name)
                .displayName(displayName)
                .texture(Item.ITEM_FOLDER + name)
                .maxStackSize(Item.SINGLE_ITEM_STACK)
                .build();
    }

    /**
     * Builds a tool that breaks blocks.
     * <p>
     * A tool is unique like every other one, it only carries the mining level and
     * the speed a rule with break times reads.
     *
     * @param id numeric item id
     * @param name technical name, also the name of the icon
     * @param displayName name shown to the player
     * @param toolLevel mining level of the tool
     * @param miningSpeed speed the tool breaks blocks with
     * @return the item definition, stacking to a single piece
     */
    private static Item toolItem(int id, String name, String displayName, int toolLevel,
            float miningSpeed) {
        return Item.builder(id, name)
                .displayName(displayName)
                .texture(Item.ITEM_FOLDER + name)
                .maxStackSize(Item.SINGLE_ITEM_STACK)
                .toolLevel(toolLevel)
                .miningSpeed(miningSpeed)
                .build();
    }
}






