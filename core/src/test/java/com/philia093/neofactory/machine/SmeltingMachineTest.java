package com.philia093.neofactory.machine;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the furnace of the game and the menu a screen would show it with.
 * <p>
 * The machine reads its recipes from the registry, so the test hands it a recipe of its
 * own and then lets time pass: fuel, progress, the result in the output slot and the way
 * the furnace cools down again are all numbers a test can read without a window.
 */
class SmeltingMachineTest {

    /** Time one craft takes in the recipe of this test. */
    private static final float CRAFT_SECONDS = 10.0f;

    private SmeltingMachine furnace;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        RecipeRegistry.clear();
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.SMELTING, "iron_ingot",
                "{ \"ingredient\": \"iron_ore\", \"result\": { \"item\": \"iron_ingot\" },"
                        + " \"time\": " + CRAFT_SECONDS + " }"));
        furnace = new SmeltingMachine();
    }

    @Test
    void withoutFuelNothingHappens() {
        furnace.inventory().set(SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 1));

        furnace.tick(5.0f);

        assertEquals(0.0f, furnace.craftProgress(), 0.001f);
        assertEquals(0.0f, furnace.burnProgress(), 0.001f);
        assertFalse(furnace.isBurning());
        assertEquals(1, furnace.inventory().get(SmeltingMachine.INPUT).count(),
                "the ore is untouched");
    }

    @Test
    void withoutSomethingToSmeltTheFuelIsKept() {
        furnace.inventory().set(SmeltingMachine.FUEL, ItemStack.of(Items.COAL, 1));

        furnace.tick(5.0f);

        assertEquals(1, furnace.inventory().get(SmeltingMachine.FUEL).count(), "no coal was wasted");
        assertFalse(furnace.isBurning());
    }

    @Test
    void anOreIsTurnedIntoAnIngot() {
        furnace.inventory().set(SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 2));
        furnace.inventory().set(SmeltingMachine.FUEL, ItemStack.of(Items.COAL, 1));

        furnace.tick(CRAFT_SECONDS / 2.0f);
        assertEquals(0.5f, furnace.craftProgress(), 0.001f);
        assertTrue(furnace.isBurning());
        assertEquals(0, furnace.inventory().get(SmeltingMachine.FUEL).count(),
                "the coal was taken and is burning now");

        furnace.tick(CRAFT_SECONDS / 2.0f);
        assertEquals(1, furnace.inventory().get(SmeltingMachine.INPUT).count(), "one ore was used");
        assertEquals(1, furnace.inventory().get(SmeltingMachine.OUTPUT).count(), "one ingot was made");
        assertTrue(furnace.inventory().get(SmeltingMachine.OUTPUT)
                .sameItem(ItemStack.of(Items.IRON_INGOT, 1)));
        assertEquals(0.0f, furnace.craftProgress(), 0.001f, "the next craft starts at zero");
    }

    @Test
    void theWorkFallsBackWhenTheFlameGoesOut() {
        furnace.inventory().set(SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 1));
        furnace.inventory().set(SmeltingMachine.FUEL, ItemStack.of(Items.STICK, 1));

        furnace.tick(4.0f);
        assertEquals(0.4f, furnace.craftProgress(), 0.001f);

        // The stick burns down during this frame, which stops the work right there.
        furnace.tick(1.0f);
        assertFalse(furnace.isBurning(), "the stick burned down");
        assertEquals(0.5f, furnace.craftProgress(), 0.001f, "one more second of work was done");

        furnace.tick(1.0f);
        assertTrue(furnace.craftProgress() < 0.5f, "the furnace cools down again");
    }

    @Test
    void aFullOutputStopsTheFurnace() {
        furnace.inventory().set(SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 1));
        furnace.inventory().set(SmeltingMachine.FUEL, ItemStack.of(Items.COAL, 1));
        furnace.inventory().set(SmeltingMachine.OUTPUT, ItemStack.of(Items.DIRT, 64));

        furnace.tick(CRAFT_SECONDS);

        assertEquals(1, furnace.inventory().get(SmeltingMachine.INPUT).count(),
                "the ore is untouched");
        assertEquals(0.0f, furnace.craftProgress(), 0.001f);
    }

    @Test
    void theSameResultIsStackedUp() {
        furnace.inventory().set(SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 1));
        furnace.inventory().set(SmeltingMachine.FUEL, ItemStack.of(Items.COAL, 1));
        furnace.inventory().set(SmeltingMachine.OUTPUT, ItemStack.of(Items.IRON_INGOT, 1));

        furnace.tick(CRAFT_SECONDS);

        assertEquals(2, furnace.inventory().get(SmeltingMachine.OUTPUT).count());
    }

    @Test
    void theMenuPlacesTheSlotsOfTheMachine() {
        MachineMenu menu = new MachineMenu(furnace, new PlayerInventory());
        ContainerLayout layout = menu.container().layout();
        Slot input = layout.slots().get(SmeltingMachine.INPUT);
        Slot fuel = layout.slots().get(SmeltingMachine.FUEL);
        Slot output = layout.slots().get(SmeltingMachine.OUTPUT);

        assertEquals(3 + PlayerInventory.SLOT_COUNT, layout.size(), "the machine and the player");
        assertFalse(input.isOutput(), "the input takes items");
        assertFalse(fuel.isOutput(), "so does the fuel slot");
        assertTrue(output.isOutput(), "the result can only be taken out");
        assertTrue(output.x() > input.x(), "the result lies behind the input");
        assertTrue(fuel.y() > input.y(), "the fuel lies below the input");
        assertTrue(layout.panelWidth() >= PanelTextures.PANEL_WIDTH,
                "the panel is at least as wide as a standard one");
        assertEquals(0.0f, menu.craftProgress(), 0.001f);
        assertEquals(furnace, menu.machine());
        assertFalse(menu.isRunning(), "nothing burns in an empty furnace");
    }
}
