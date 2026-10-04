package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.machine.AlloyFurnaceMachine;
import com.philia093.neofactory.machine.CompressorMachine;
import com.philia093.neofactory.machine.ExtractorMachine;
import com.philia093.neofactory.machine.ForgeHammerMachine;
import com.philia093.neofactory.machine.GrinderMachine;
import com.philia093.neofactory.machine.HighPressureAlloyFurnaceMachine;
import com.philia093.neofactory.machine.HighPressureBoilerMachine;
import com.philia093.neofactory.machine.HighPressureCompressorMachine;
import com.philia093.neofactory.machine.HighPressureExtractorMachine;
import com.philia093.neofactory.machine.HighPressureForgeHammerMachine;
import com.philia093.neofactory.machine.HighPressureGrinderMachine;
import com.philia093.neofactory.machine.HighPressureSteamFurnaceMachine;
import com.philia093.neofactory.machine.BatteryBoxMachine;
import com.philia093.neofactory.machine.BatteryBoxes;
import com.philia093.neofactory.machine.DiodeMachine;
import com.philia093.neofactory.machine.Diodes;
import com.philia093.neofactory.machine.MachineFamilies;
import com.philia093.neofactory.machine.SmeltingMachine;
import com.philia093.neofactory.machine.SteamBoilerMachine;
import com.philia093.neofactory.machine.SteamFurnaceMachine;
import com.philia093.neofactory.machine.SteamTurbineMachine;
import com.philia093.neofactory.machine.TurbineTier;

/**
 * Every block entity type the game knows.
 * <p>
 * Names are permanent: a stored chunk refers to them, so a type that ships keeps its
 * name forever. A type belongs to the block that declares it as well, so a new machine
 * is added in three places - the block in {@code Blocks}, the item in {@code Items} and
 * the type here - and a new system of its own brings its own table next to this one.
 * <p>
 * The types are registered once during startup, before
 * {@link BlockEntityRegistry#freeze()} is called, see
 * {@link com.philia093.neofactory.NeoFactoryGame#create()}.
 */
public final class BlockEntityTypes {

    /** The furnace, a machine that smelts what it is given. */
    public static final BlockEntityType FURNACE = new BlockEntityType("furnace",
            type -> new MachineBlockEntity(type, new SmeltingMachine()));

    /** The bronze boiler, the machine that turns water into steam. */
    public static final BlockEntityType BRONZE_BOILER = new BlockEntityType("bronze_boiler",
            type -> new MachineBlockEntity(type, new SteamBoilerMachine()));

    /** The steam furnace, the machine that smelts ore with the steam of the boiler. */
    public static final BlockEntityType STEAM_FURNACE = new BlockEntityType("steam_furnace",
            type -> new MachineBlockEntity(type, new SteamFurnaceMachine()));

    /** The alloy furnace, the machine that melts two metals into one. */
    public static final BlockEntityType ALLOY_FURNACE = new BlockEntityType("alloy_furnace",
            type -> new MachineBlockEntity(type, new AlloyFurnaceMachine()));

    /** The grinder, the machine that turns ore into dust. */
    public static final BlockEntityType GRINDER = new BlockEntityType("grinder",
            type -> new MachineBlockEntity(type, new GrinderMachine()));

    /** The compressor, the machine that presses an item together. */
    public static final BlockEntityType COMPRESSOR = new BlockEntityType("compressor",
            type -> new MachineBlockEntity(type, new CompressorMachine()));

    /** The extractor, the machine that squeezes what is in an item out of it. */
    public static final BlockEntityType EXTRACTOR = new BlockEntityType("extractor",
            type -> new MachineBlockEntity(type, new ExtractorMachine()));

    /** The forge hammer, the machine that beats an ingot into shape. */
    public static final BlockEntityType FORGE_HAMMER = new BlockEntityType("forge_hammer",
            type -> new MachineBlockEntity(type, new ForgeHammerMachine()));

    /** The boiler of the age of steel, which makes steam three hundred millibuckets a second. */
    public static final BlockEntityType STEEL_BOILER = new BlockEntityType("steel_boiler",
            type -> new MachineBlockEntity(type, new HighPressureBoilerMachine()));

    /** The steam furnace of steel, which smelts in half the time of the one of bronze. */
    public static final BlockEntityType STEEL_STEAM_FURNACE = new BlockEntityType("steel_steam_furnace",
            type -> new MachineBlockEntity(type, new HighPressureSteamFurnaceMachine()));

    /** The alloy furnace of steel. */
    public static final BlockEntityType STEEL_ALLOY_FURNACE = new BlockEntityType("steel_alloy_furnace",
            type -> new MachineBlockEntity(type, new HighPressureAlloyFurnaceMachine()));

    /** The grinder of steel. */
    public static final BlockEntityType STEEL_GRINDER = new BlockEntityType("steel_grinder",
            type -> new MachineBlockEntity(type, new HighPressureGrinderMachine()));

    /** The compressor of steel. */
    public static final BlockEntityType STEEL_COMPRESSOR = new BlockEntityType("steel_compressor",
            type -> new MachineBlockEntity(type, new HighPressureCompressorMachine()));

    /** The extractor of steel. */
    public static final BlockEntityType STEEL_EXTRACTOR = new BlockEntityType("steel_extractor",
            type -> new MachineBlockEntity(type, new HighPressureExtractorMachine()));

    /** The forge hammer of steel. */
    public static final BlockEntityType STEEL_FORGE_HAMMER = new BlockEntityType("steel_forge_hammer",
            type -> new MachineBlockEntity(type, new HighPressureForgeHammerMachine()));

    /** A pipe, which carries the little fluid of its tube and the valve of every side. */
    public static final BlockEntityType PIPE = new BlockEntityType("pipe",
            type -> new PipeBlockEntity(type));

    /** A cable, whose state is the mask of the sides it joins, see {@code Cables}. */
    public static final BlockEntityType CABLE = new BlockEntityType("cable",
            type -> new CableBlockEntity(type));

    /**
     * The steam turbine of the low voltage, the first machine of the game that makes power.
     * <p>
     * Three types for the three tiers of the machine, because the tier is what its numbers come from and a
     * block entity holds one machine: each of them carries the tank of steam, the buffer of its tier and the
     * vent the spent steam goes out of, see {@link SteamTurbineMachine} and {@link TurbineTier}.
     */
    public static final BlockEntityType STEAM_TURBINE_LV = new BlockEntityType("steam_turbine_lv",
            type -> new MachineBlockEntity(type, new SteamTurbineMachine(TurbineTier.LV)));

    /** The steam turbine of the middle voltage. */
    public static final BlockEntityType STEAM_TURBINE_MV = new BlockEntityType("steam_turbine_mv",
            type -> new MachineBlockEntity(type, new SteamTurbineMachine(TurbineTier.MV)));

    /** The steam turbine of the high voltage. */
    public static final BlockEntityType STEAM_TURBINE_HV = new BlockEntityType("steam_turbine_hv",
            type -> new MachineBlockEntity(type, new SteamTurbineMachine(TurbineTier.HV)));

    /**
     * A chest, the first block that keeps what a player puts in it.
     * <p>
     * Not a machine: it holds a bag of items and does no work with them, so it carries the
     * container entity and not the one a machine is built on, see {@link ChestBlockEntity}.
     */
    public static final BlockEntityType CHEST = new BlockEntityType("chest",
            type -> new ChestBlockEntity(type));

    /**
     * The table of the workshop, the first container that works on what it holds.
     * <p>
     * It is a container and not a machine: it holds a field of nine cells and makes nothing until a
     * player takes the product of the recipe those cells hold, see {@link CraftingTableBlockEntity}.
     */
    public static final BlockEntityType CRAFTING_TABLE = new BlockEntityType("crafting_table",
            type -> new CraftingTableBlockEntity(type));

    private static boolean registered;

    private BlockEntityTypes() {
        // Utility class: never instantiated.
    }

    /** Registers every block entity type, called once during startup. */
    public static void registerAll() {
        if (registered) {
            return;
        }
        BlockEntityRegistry.register(FURNACE);
        BlockEntityRegistry.register(CABLE);
        BlockEntityRegistry.register(BRONZE_BOILER);
        BlockEntityRegistry.register(STEAM_FURNACE);
        BlockEntityRegistry.register(ALLOY_FURNACE);
        BlockEntityRegistry.register(GRINDER);
        BlockEntityRegistry.register(COMPRESSOR);
        BlockEntityRegistry.register(EXTRACTOR);
        BlockEntityRegistry.register(FORGE_HAMMER);
        BlockEntityRegistry.register(STEEL_BOILER);
        BlockEntityRegistry.register(STEEL_STEAM_FURNACE);
        BlockEntityRegistry.register(STEEL_ALLOY_FURNACE);
        BlockEntityRegistry.register(STEEL_GRINDER);
        BlockEntityRegistry.register(STEEL_COMPRESSOR);
        BlockEntityRegistry.register(STEEL_EXTRACTOR);
        BlockEntityRegistry.register(STEEL_FORGE_HAMMER);
        BlockEntityRegistry.register(STEAM_TURBINE_LV);
        BlockEntityRegistry.register(STEAM_TURBINE_MV);
        BlockEntityRegistry.register(STEAM_TURBINE_HV);
        BlockEntityRegistry.register(PIPE);
        BlockEntityRegistry.register(CHEST);
        BlockEntityRegistry.register(CRAFTING_TABLE);

        // The eighteen machines of the line of the power, one type per family and tier. They are not written
        // out here because there are eighteen of them: the table of the families names every one of them and
        // hands the machine behind the block over, see MachineFamilies.
        for (MachineFamilies.Family family : MachineFamilies.all()) {
            for (Voltage tier : MachineFamilies.TIERS) {
                BlockEntityRegistry.register(new BlockEntityType(family.nameOf(tier),
                        type -> new MachineBlockEntity(type, family.machine(tier))));
            }
        }

        // The twelve boxes of cells, one type per size and tier, the same way: the table of the boxes names
        // every one of them and the machine behind a block is a box of that size, see BatteryBoxes.
        for (Voltage tier : BatteryBoxes.tiers()) {
            for (int cells : BatteryBoxes.CELLS) {
                BlockEntityRegistry.register(new BlockEntityType(BatteryBoxes.nameOf(tier, cells),
                        type -> new MachineBlockEntity(type, new BatteryBoxMachine(tier, cells))));
            }
        }
        // The fifteen diodes of the industry, one type per width and tier, the same way: the table of the
        // diodes names every one of them and the machine behind a block is a diode of that tier and width,
        // see Diodes. A diode is the one block of the line that is not a machine of the usual kind: it carries
        // a line of cables through itself one way and holds nothing at all.
        for (Voltage tier : Diodes.tiers()) {
            for (int width : Diodes.WIDTHS) {
                BlockEntityRegistry.register(new BlockEntityType(Diodes.nameOf(tier, width),
                        type -> new DiodeBlockEntity(type, new DiodeMachine(tier, width))));
            }
        }
        registered = true;
    }
}
