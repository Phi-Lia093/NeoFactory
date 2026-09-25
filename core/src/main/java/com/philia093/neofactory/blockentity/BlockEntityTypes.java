package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.machine.SmeltingMachine;
import com.philia093.neofactory.machine.SteamBoilerMachine;

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
        BlockEntityRegistry.register(BRONZE_BOILER);
        registered = true;
    }
}
