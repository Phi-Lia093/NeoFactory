package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ChemicalItems;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.recipe.ChemicalRecipe;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two machines of the chemistry against the routes the game ships.
 * <p>
 * A route is paid the way it is written: what is a solid stands in a slot as whole pieces, what flows
 * stands in a tank in millibuckets, and the products leave the same way. The test hands a reactor the
 * synthesis gas of carbon and water and lets the clock run, then does the same to an electrolyzer with
 * the water a current splits, so both halves of the payment are read without a window.
 */
class ChemicalMachineTest {

    private static final Path RECIPES = Path.of("..", "assets", "recipes");

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        RecipeRegistry.clear();
        RecipeRegistry.register(route(RecipeType.CHEMICAL_REACTING, "syngas"));
        RecipeRegistry.register(route(RecipeType.ELECTROLYSIS, "water_electrolysis"));
    }

    /** One route of the game, read the way the game reads it. */
    private static ChemicalRecipe route(RecipeType type, String name) {
        try {
            return (ChemicalRecipe) RecipeLoader.parse(type, name,
                    Files.readString(RECIPES.resolve(type.name()).resolve(name + ".json"),
                            StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("the route " + name + " cannot be read", e);
        }
    }

    /** Fills the buffer of a machine, so that it has the power to work. */
    private static void power(ElectricMachine machine) {
        machine.buffer().setAmount(machine.bufferCapacity());
    }

    /**
     * Lets a machine work for a while, a twentieth of a second at a time.
     * <p>
     * A frame of twelve seconds is more than the buffer of a machine holds, and a machine of the line is
     * fed a frame at a time by the line it stands on: the test feeds it the same way, one tick at a time,
     * see {@link ElectricMachine#update(float)}.
     */
    private static void run(ElectricMachine machine, float seconds) {
        int ticks = Math.round(seconds * 20.0f);
        for (int tick = 0; tick < ticks; tick++) {
            machine.tick(0.05f);
            power(machine);
        }
    }

    @Test
    void aReactorTurnsCarbonAndWaterIntoTheSynthesisGas() {
        ChemicalReactorMachine reactor = new ChemicalReactorMachine(
                com.philia093.neofactory.cable.Voltage.LOW);
        reactor.inventory().set(ChemicalReactorMachine.FIRST_INPUT,
                ItemStack.of(ChemicalItems.itemOf(chemical("carbon")), 1));
        reactor.tank(0).storage().fill(Fluids.WATER, 100, false);
        power(reactor);

        run(reactor, 40.0f);

        assertTrue(reactor.inventory().get(ChemicalReactorMachine.FIRST_INPUT).isEmpty(),
                "the dust the route asked for was taken out of its slot");
        assertEquals(0, reactor.tank(0).storage().amount(), "and the water out of its tank");
        assertEquals(100, reactor.tank(2).storage().amount(),
                "the monoxide of the route arrived in the first output tank");
        assertEquals(100, reactor.tank(3).storage().amount(),
                "and the hydrogen of it in the second");
    }

    @Test
    void anElectrolyzerSplitsWaterIntoItsTwoGases() {
        ElectrolyzerMachine electrolyzer = new ElectrolyzerMachine(
                com.philia093.neofactory.cable.Voltage.LOW);
        electrolyzer.tank(0).storage().fill(Fluids.WATER, 200, false);
        power(electrolyzer);

        run(electrolyzer, 40.0f);

        assertEquals(0, electrolyzer.tank(0).storage().amount(), "the water of the route was spent");
        assertEquals(200, electrolyzer.tank(1).storage().amount(), "the hydrogen of the route");
        assertEquals(100, electrolyzer.tank(2).storage().amount(), "and the oxygen of it");
    }

    @Test
    void aReactorWithNothingToRunDoesNothing() {
        ChemicalReactorMachine reactor = new ChemicalReactorMachine(
                com.philia093.neofactory.cable.Voltage.LOW);
        reactor.inventory().set(ChemicalReactorMachine.FIRST_INPUT, ItemStack.of(Items.STICK, 1));
        power(reactor);

        run(reactor, 40.0f);

        assertEquals(1, reactor.inventory().get(ChemicalReactorMachine.FIRST_INPUT).count(),
                "a stick is no route of the industry and stays where it stands");
    }

    @Test
    void aReactorAccountsForEveryPotItRuns() {
        // Butane is a pot no written route of the industry names, so the simple side of the table is asked
        // for it. Whether the rule that fits it is allowed to run or is refused for a product the game
        // cannot hand over, one thing may never happen: molecules that leave the tank and arrive nowhere.
        ChemicalReactorMachine reactor = new ChemicalReactorMachine(
                com.philia093.neofactory.cable.Voltage.LOW);
        var butane = com.philia093.neofactory.fluid.Fluids.byName("butane");
        assertTrue(butane != null, "the catalog holds the fluid of butane");
        reactor.tank(0).storage().fill(butane, 1_000, false);
        power(reactor);

        run(reactor, 60.0f);

        int spent = 1_000 - reactor.tank(0).storage().amount();
        int arrived = reactor.tank(2).storage().amount() + reactor.tank(3).storage().amount();
        assertTrue(spent == 0 || arrived > 0,
                "a pot that was run left its molecules in an output tank, and one that was not is untouched");
    }

    @Test
    void aReactorLeavesAPotAloneWhenNoWrittenRouteAndNoSimpleRuleAnswersIt() {
        ChemicalReactorMachine reactor = new ChemicalReactorMachine(
                com.philia093.neofactory.cable.Voltage.LOW);
        var ethanol = com.philia093.neofactory.fluid.Fluids.byName("ethanol");
        assertTrue(ethanol != null, "the catalog holds the fluid of ethanol");
        reactor.tank(0).storage().fill(ethanol, 1_000, false);
        power(reactor);

        run(reactor, 60.0f);

        assertEquals(1_000, reactor.tank(0).storage().amount(),
                "the ethanol of a pot nothing answers is left exactly where it stands");
    }

    /** A substance of the catalog by name. */
    private static com.philia093.neofactory.chemistry.Chemical chemical(String name) {
        com.philia093.neofactory.chemistry.Substance substance =
                com.philia093.neofactory.chemistry.Substances.starter().byName(name);
        assertNotNull(substance, "the catalog knows " + name);
        return substance.chemical();
    }
}
