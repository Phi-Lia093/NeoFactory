package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.block.model.BlockModel;
import com.philia093.neofactory.block.model.ModelRegistry;
import com.philia093.neofactory.block.state.BlockStateTable;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.BlockEntityType;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the twelve boxes of cells against the game: their blocks, their items and the entities behind them.
 * <p>
 * A box is registered the way every machine of the industry is - a block, an item that places it and a block
 * entity that hands the machine behind the block over - so what is checked here is that the table of the boxes
 * is really walked by all three of them and that the run of the ids is tight, see {@code Blocks},
 * {@code Items} and {@code BlockEntityTypes}. <b>A box carries no state at all</b>, which is the one thing it
 * has that no other machine of the game has: it is turned towards nothing, so its block names no facing and is
 * drawn from one model of six faces of the casing of its tier, and the two sides a player gives the power to
 * are drawn over that casing by the block entity of the machine, see {@link MachineCasing}.
 */
class BatteryBoxesTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everySizeOfEveryTierIsABlockAnItemAndAnEntityOfItsOwn() {
        int blockId = Blocks.BATTERY_BOX_FIRST_ID;
        int itemId = Items.BATTERY_BOX_FIRST_ID;

        for (Voltage tier : BatteryBoxes.tiers()) {
            for (int cells : BatteryBoxes.CELLS) {
                String name = BatteryBoxes.nameOf(tier, cells);
                Block block = BlockRegistry.byName(name);
                Item item = ItemRegistry.byName(name);

                assertNotNull(block, name + " is a box of the table and no block of the game");
                assertNotNull(item, name + " is a box of the table and no item of the game");
                assertEquals(blockId++, block.id(), "the run of the blocks is tight, see BATTERY_BOX_FIRST_ID");
                assertEquals(itemId++, item.id(), "and so is the run of the items");
                assertSame(block, item.block(), name + " is placed by its own item");
                assertTrue(item.isBlockItem());

                assertEquals(MachineCasing.pictureOf(tier), block.texture(),
                        name + " is built of the casing of its tier");
                assertTrue(block.isDrawable());
                assertFalse(block.carriesFluid(), "a box holds no tank and no pipe is built towards it");
                assertEquals(BlockStateTable.NONE, block.states(), "a box is turned towards nothing");
                assertFalse(block.states().hasProperty(MachineBlockEntity.FACING));

                // The shape of the box is a model of its own, one cube of six faces of the casing.
                BlockModel model = ModelRegistry.byName(name);
                assertNotNull(model, name + " has no model file");
                assertEquals(1, model.boxes().size());
                assertEquals(1, model.pictures().size(), "one picture and no overlay on the block itself");
                assertEquals(MachineCasing.pictureOf(tier), model.pictures().iterator().next());
            }
        }

        assertEquals(Blocks.DIODE_FIRST_ID, blockId,
                "the boxes stand behind the machines of the line and in front of the diodes");
        assertEquals(Items.BATTERY_BOX_FIRST_ID + BatteryBoxes.COUNT, itemId,
                "and in front of the items of the materials");
    }

    @Test
    void theBlockEntityOfABoxHandsTheMachineOfThatBoxOver() {
        for (Voltage tier : BatteryBoxes.tiers()) {
            for (int cells : BatteryBoxes.CELLS) {
                String name = BatteryBoxes.nameOf(tier, cells);
                BlockEntityType type = BlockEntityRegistry.byName(name);

                assertNotNull(type, name + " is a block of the game and names no entity type");
                assertEquals(name, type.name());
                assertEquals(name, BlockRegistry.byName(name).blockEntityTypeName());

                assertTrue(type.create() instanceof MachineBlockEntity, name + " is a machine");
                MachineBlockEntity entity = (MachineBlockEntity) type.create();
                assertTrue(entity.machine() instanceof BatteryBoxMachine, name + " hands a box over");
                BatteryBoxMachine box = (BatteryBoxMachine) entity.machine();
                assertEquals(cells, box.cells());
                assertEquals(tier, box.tier());
            }
        }
    }
}
