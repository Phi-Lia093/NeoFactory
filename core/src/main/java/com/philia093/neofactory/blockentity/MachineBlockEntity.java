package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.block.state.BlockStateTable;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The block entity of a machine: what a machine looks like from the world.
 * <p>
 * The class is the bridge between the two halves of a machine. The world only knows
 * block entities - it ticks them and stores them with their chunk - while a machine
 * knows nothing about the world at all and can therefore be tested without one. This
 * class hands the tick on, drops the state into the save game and hands the content of
 * the machine to the world while its block is broken.
 * <p>
 * A machine that needs to look at its neighbours overrides {@code update} in a subclass
 * and keeps the world it was given.
 */
public class MachineBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Property a block of a machine uses to say that the machine works.
     * <p>
     * A block that carries it draws another picture while the machine runs - the mouth of a boiler glows
     * when it burns - which is what the state of a cell is for, see {@link BlockStateTable}. A block
     * without it is simply never relit.
     */
    public static final String LIT = "lit";

    private final Machine machine;

    /**
     * Creates the block entity of a machine.
     *
     * @param type type of this block entity
     * @param machine machine behind this block
     */
    public MachineBlockEntity(BlockEntityType type, Machine machine) {
        super(type);
        this.machine = Objects.requireNonNull(machine, "machine");
    }

    /** Machine behind this block. */
    public Machine machine() {
        return machine;
    }

    @Override
    protected void update(World world, float delta) {
        machine.tick(delta);
        if (machine.isExploded()) {
            ruin(world);
            return;
        }
        updateLitState(world);
    }

    /**
     * Takes the machine out of the world after it ruined itself.
     * <p>
     * The block goes and the entity with it, and everything the machine held goes as well: the cell is
     * replaced without a loot table being asked and without {@link #onBroken} being called, so a boiler that
     * exploded leaves nothing behind but the hole it stood in - neither its slots nor its tanks are handed to
     * the player. Only the block itself is destroyed; the world around it is untouched, which is the part of
     * an explosion that is not written yet.
     */
    private void ruin(World world) {
        LOGGER.info("The {} at ({}, {}, {}) ruined itself", machine.name(), x(), y(), z());
        world.setBlock(x(), y(), z(), Blocks.AIR);
    }

    /**
     * Says in the state of the cell whether the machine works.
     * <p>
     * A machine that runs is drawn with the glowing picture of its front, see {@link #LIT}: the state
     * travels with the chunk like the block itself, so a world that is opened again shows a boiler that
     * still burns. The lookup only runs for a block that carries the property at all, so the cost is one
     * map lookup per tick for every machine of the game.
     */
    private void updateLitState(World world) {
        BlockStateTable states = world.getBlock(x(), y(), z()).states();
        if (!states.hasProperty(LIT)) {
            return;
        }
        int state = world.getState(x(), y(), z());
        Map<String, String> values = new LinkedHashMap<>(states.decode(state));
        String wanted = machine.isRunning() ? "true" : "false";
        if (wanted.equals(values.get(LIT))) {
            return;
        }
        values.put(LIT, wanted);
        world.setState(x(), y(), z(), states.stateOf(values));
    }

    @Override
    protected void writeOwnData(NbtCompound data) {
        machine.save(data);
    }

    @Override
    protected void readOwnData(NbtCompound data) {
        machine.load(data);
    }

    @Override
    public void onBroken(ItemDrops drops, float worldX, float worldZ) {
        machine.dumpItems(drops, worldX, worldZ);
    }

    @Override
    public String toString() {
        return "MachineBlockEntity(" + type().name() + " " + machine + ")";
    }
}
