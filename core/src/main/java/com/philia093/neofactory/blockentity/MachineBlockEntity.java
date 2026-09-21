package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;

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
    public void onBroken(ItemDrops drops, float worldX, float worldY) {
        machine.dumpItems(drops, worldX, worldY);
    }

    @Override
    public String toString() {
        return "MachineBlockEntity(" + type().name() + " " + machine + ")";
    }
}
