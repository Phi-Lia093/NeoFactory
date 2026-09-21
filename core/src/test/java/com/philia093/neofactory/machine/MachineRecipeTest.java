package com.philia093.neofactory.machine;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.recipe.Ingredient;
import com.philia093.neofactory.recipe.RecipeGrid;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.TickClock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the contract of a machine is enough for the largest machine of the game.
 * <p>
 * The test builds the machine the factory systems will end up with - a reactor with two
 * item inputs, a fluid input, two item outputs, a fluid output, an upgrade slot and a
 * buffer - out of exactly the pieces a furnace uses, and lets it run. None of it is part
 * of the game yet, and that is the point: the machine at the bottom of this file shows
 * that a machine that large needs no new contract, only more slots, more tanks and a
 * recipe that makes two things at once.
 */
class MachineRecipeTest {

    /** Seconds one reaction takes. */
    private static final float REACTION_SECONDS = 5.0f;

    /** Energy one reaction costs. */
    private static final int REACTION_ENERGY = 100;

    /** Energy the buffer of the test machine is filled with. */
    private static final int BUFFER = 1000;

    private TestReactor reactor;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        RecipeRegistry.clear();
        RecipeRegistry.register(new ReactorRecipe());
        reactor = new TestReactor();
    }

    @Test
    void theLoopOfALargeMachineRuns() {
        fillInputs();

        tick(reactor, 120);

        assertEquals(2, reactor.inventory().get(TestReactor.INGOT_OUTPUT).count(),
                "the first product did not arrive");
        assertEquals(1, reactor.inventory().get(TestReactor.SLAG_OUTPUT).count(),
                "the second product did not arrive");
        assertEquals(0, reactor.inventory().get(TestReactor.MAIN).count(), "the ore was not used");
        assertEquals(0, reactor.inventory().get(TestReactor.SECONDARY).count(), "the sand was not used");
        assertEquals(FluidType.LAVA, reactor.tank(1).storage().fluid(),
                "the fluid product did not arrive");
        assertEquals(50, reactor.tank(1).storage().amount());
        assertEquals(100, reactor.tank(0).storage().amount(),
                "the recipe takes 100 of the 200 units in the tank");
    }

    @Test
    void withoutEnergyTheMachineWaits() {
        fillInputs();
        reactor.energy().extract(BUFFER, false);

        tick(reactor, 200);

        assertEquals(0.0f, reactor.craftProgress(), 0.001f);
        assertEquals(1, reactor.inventory().get(TestReactor.MAIN).count(), "the machine ran anyway");
        assertEquals(0, reactor.tank(1).storage().amount());
    }

    @Test
    void theFluidIsPartOfTheRecipe() {
        reactor.inventory().set(TestReactor.MAIN, ItemStack.of(Items.IRON_ORE, 1));
        reactor.inventory().set(TestReactor.SECONDARY, ItemStack.of(Items.SAND, 1));
        reactor.energy().receive(BUFFER, false);
        // No water in the tank: the items alone are not enough.

        tick(reactor, 200);

        assertEquals(1, reactor.inventory().get(TestReactor.MAIN).count(),
                "the machine ran without the fluid it needs");
        assertEquals(0.0f, reactor.craftProgress(), 0.001f);
    }

    @Test
    void theUpgradeSlotIsNotPartOfTheRecipe() {
        fillInputs();
        // A module in the slot the machine reads for itself: it belongs to the machine and
        // not to a recipe, so it must not disturb one.
        reactor.inventory().set(TestReactor.UPGRADE, ItemStack.of(Items.REDSTONE_DUST, 3));

        tick(reactor, 120);

        assertEquals(2, reactor.inventory().get(TestReactor.INGOT_OUTPUT).count());
        assertEquals(3, reactor.inventory().get(TestReactor.UPGRADE).count(),
                "the module was touched by a recipe");
    }

    @Test
    void aFullOutputStopsTheMachine() {
        fillInputs();
        reactor.inventory().set(TestReactor.INGOT_OUTPUT, ItemStack.of(Items.DIRT, 64));
        reactor.inventory().set(TestReactor.SLAG_OUTPUT, ItemStack.of(Items.DIRT, 64));

        tick(reactor, 200);

        assertEquals(1, reactor.inventory().get(TestReactor.MAIN).count(),
                "the machine ate its input although the products had no place");
        assertEquals(0.0f, reactor.craftProgress(), 0.001f);
    }

    @Test
    void theEnergyIsPaidForTheCraftAndNothingMore() {
        fillInputs();
        int before = reactor.energy().amount();

        tick(reactor, 100);

        int paid = before - reactor.energy().amount();
        assertTrue(paid >= REACTION_ENERGY && paid <= REACTION_ENERGY + 1,
                "the machine paid " + paid + " for a craft that costs " + REACTION_ENERGY);
    }

    @Test
    void theWorkTravelsThroughTheStateOfTheMachine() {
        fillInputs();
        tick(reactor, 50);

        NbtCompound state = new NbtCompound("State");
        reactor.save(state);

        TestReactor restored = new TestReactor();
        restored.load(state);

        assertEquals(reactor.craftProgress(), restored.craftProgress(), 0.001f,
                "the work of the machine was lost");
        assertEquals(reactor.energy().amount(), restored.energy().amount());
        assertEquals(FluidType.WATER, restored.tank(0).storage().fluid());
        assertEquals(200, restored.tank(0).storage().amount());
        assertEquals(1, restored.inventory().get(TestReactor.MAIN).count());
    }

    /** Fills the two input slots, the fluid tank and the buffer of the reactor. */
    private void fillInputs() {
        reactor.inventory().set(TestReactor.MAIN, ItemStack.of(Items.IRON_ORE, 1));
        reactor.inventory().set(TestReactor.SECONDARY, ItemStack.of(Items.SAND, 1));
        reactor.tank(0).storage().fill(FluidType.WATER, 200, false);
        reactor.energy().receive(BUFFER, false);
    }

    /** Runs a machine for a number of ticks, the way the world would. */
    private static void tick(Machine machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            machine.tick(TickClock.TICK_SECONDS);
        }
    }

    /**
     * A recipe of the shape a chemical reactor works with: two items and a fluid in, two
     * items and a fluid out, for energy.
     * <p>
     * It declares its ingredients and its products and nothing else - matching, consuming
     * and producing come from {@link MachineRecipe}.
     */
    private static final class ReactorRecipe implements MachineRecipe {

        @Override
        public String name() {
            return "test_reaction";
        }

        @Override
        public RecipeType type() {
            return RecipeType.SMELTING;
        }

        @Override
        public float seconds() {
            return REACTION_SECONDS;
        }

        @Override
        public int energy() {
            return REACTION_ENERGY;
        }

        @Override
        public ItemStack result() {
            return ItemStack.of(Items.IRON_INGOT, 2);
        }

        @Override
        public List<ItemStack> products() {
            return List.of(ItemStack.of(Items.IRON_INGOT, 2), ItemStack.of(Items.GRAVEL, 1));
        }

        @Override
        public List<FluidIngredient> fluidIngredients() {
            return List.of(FluidIngredient.of(FluidType.WATER, 100));
        }

        @Override
        public List<FluidIngredient> fluidProducts() {
            return List.of(FluidIngredient.of(FluidType.LAVA, 50));
        }

        @Override
        public boolean matches(RecipeGrid grid) {
            // The input of the machine offers both of its input slots in the order they
            // were declared: the ore first, the sand behind it.
            return grid.width() >= 2
                    && Ingredient.of(Items.IRON_ORE).matches(grid.get(0, 0))
                    && Ingredient.of(Items.SAND).matches(grid.get(1, 0));
        }

        @Override
        public void consume(RecipeGrid grid) {
            takeOne(grid.get(0, 0));
            takeOne(grid.get(1, 0));
        }

        private static void takeOne(ItemStack stack) {
            stack.setCount(stack.count() - 1);
        }
    }

    /**
     * The machine of this test: the shape a chemical reactor has.
     * <p>
     * It describes its slots, its tanks and the recipes it reads - everything else comes
     * from {@link RecipeMachine}.
     */
    private static final class TestReactor extends RecipeMachine {

        private static final int MAIN = 0;
        private static final int SECONDARY = 1;
        private static final int UPGRADE = 2;
        private static final int INGOT_OUTPUT = 4;
        private static final int SLAG_OUTPUT = 5;

        /** Screen of a reaction: two slots in, two slots out, two upgrades and a tank each way. */
        private static final MachineScreen SCREEN = new MachineScreen("Reactor",
                ProgressKind.CHEMICAL, List.of(SlotKind.SMELTING, SlotKind.SMELTING),
                List.of(SlotKind.GENERIC, SlotKind.GENERIC), 1, 1, true);

        private TestReactor() {
            super(SCREEN,
                    new MachineInventory(MachineInventory.Role.INPUT, MachineInventory.Role.INPUT,
                            MachineInventory.Role.UPGRADE, MachineInventory.Role.UPGRADE,
                            MachineInventory.Role.OUTPUT, MachineInventory.Role.OUTPUT,
                            MachineInventory.Role.CONFIGURE),
                    new SimpleEnergyStorage(BUFFER),
                    List.of(RecipeType.SMELTING),
                    MachineTank.of(1000, MachineTank.Role.INPUT),
                    MachineTank.of(1000, MachineTank.Role.OUTPUT));
        }
    }
}
