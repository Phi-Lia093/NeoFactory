package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * The machines of the line of the power, one row per family of the art.
 * <p>
 * <b>A machine of the line exists once per tier and not once in the code.</b> The furnace of the low voltage,
 * the one of the middle voltage and the one of the high voltage are the same machine built of another casing
 * over a line of another strength, so what is written down here is what makes the eighteen of them: the
 * family of the art they are drawn with, the group of recipes they read, the slots they offer and the current
 * they take. Everything else follows from the row and the tier - the name they are registered under, the
 * title of their screen, the picture of every face - so the three tables of the game can be filled by walking
 * this one, see {@code Blocks}, {@code Items} and {@code BlockEntityTypes}.
 * <p>
 * <b>A row is a family and a tier is a machine.</b> The name of a machine is its family and the name of its
 * tier - {@code macerator_lv} - which is at once the name of its block, of its item and of its block entity,
 * and the title a player reads is {@code LV Macerator}, the way the machines of the age of steam carry the
 * prefix of their pressure, see {@link MachinePressure#title(String)}.
 * <p>
 * The class of a machine of the line names its row and is built from it, see {@link ElectricMaceratorMachine}:
 * what a player owns is a machine of a family and a tier, and what a later machine of the same family adds -
 * the current of an arc furnace, the tanks of a chemical reactor - is written in the class and not here.
 */
public final class MachineFamilies {

    /**
     * A family of machines of the line.
     * <p>
     * The row says what every machine of the family is: the name of the folder of its art, what a player reads
     * at it, the group of recipes it works through, the slots it offers, the current it takes at most and the
     * type it is created as. A machine of the family is one tier of it, so a row is asked for the name, the
     * title, the picture and the screen of a tier and answers with what that tier makes of it.
     *
     * @param name name of the family, which is the folder of its art and the first half of a machine's name
     * @param display what a player reads at a machine of the family, without the tier in front of it
     * @param recipeTypes types of recipe a machine of the family reads, at least one
     * @param inputs kind of every input slot, in the order the layout draws them
     * @param outputs kind of every output slot, in the order the layout draws them
     * @param maxAmps largest current a machine of the family takes, in amperes at its own tier
     * @param factory the type a machine of the family is created as
     */
    public record Family(String name, String display, List<RecipeType> recipeTypes, List<SlotKind> inputs,
            List<SlotKind> outputs, int maxAmps, Function<Voltage, ElectricMachine> factory) {

        public Family {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(display, "display");
            recipeTypes = List.copyOf(Objects.requireNonNull(recipeTypes, "recipeTypes"));
            inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
            outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
            Objects.requireNonNull(factory, "factory");
            if (recipeTypes.isEmpty()) {
                throw new IllegalArgumentException("A machine works through a recipe: " + name);
            }
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("A machine works on something: " + name);
            }
        }

        /** Name of the machine of a tier, which is its block, its item and its entity all at once. */
        public String nameOf(Voltage tier) {
            return name + '_' + Objects.requireNonNull(tier, "tier").fileName();
        }

        /** Title of the screen of the machine of a tier, such as {@code LV Macerator}. */
        public String titleOf(Voltage tier) {
            return Objects.requireNonNull(tier, "tier").fileName().toUpperCase(Locale.ROOT) + ' ' + display;
        }

        /**
         * The art of one face of the machine of a tier, relative to {@code blocks/}.
         *
         * @param tier tier the machine was built for
         * @param face face of the machine, such as {@code front}
         * @return the name of the picture, the way a block names its texture
         */
        public String pictureOf(Voltage tier, String face) {
            return "basicmachines/" + name + '/' + nameOf(tier) + '_' + face;
        }

        /**
         * How the machine of a tier is shown.
         * <p>
         * Every machine of the line is drawn in the grey panel of the age of electricity and holds no tank:
         * the power it runs on arrives over a line and never in a bucket, see {@link MachineStyle#NORMAL}.
         */
        public MachineScreen screenOf(Voltage tier) {
            return new MachineScreen(titleOf(tier), MachineStyle.NORMAL, ProgressKind.GENERIC, inputs, outputs,
                    0, 0, false);
        }

        /**
         * The slots of a machine of this family.
         * <p>
         * Every input stands before the output it feeds, which is the order the layout draws them in and the
         * order a recipe finds them, see {@link MachineInventory}.
         */
        public MachineInventory inventory() {
            List<MachineInventory.Role> roles = new ArrayList<>();
            for (int slot = 0; slot < inputs.size(); slot++) {
                roles.add(MachineInventory.Role.INPUT);
            }
            for (int slot = 0; slot < outputs.size(); slot++) {
                roles.add(MachineInventory.Role.OUTPUT);
            }
            return new MachineInventory(roles.toArray(new MachineInventory.Role[0]));
        }

        /**
         * The machine of a tier, created as the type this row names.
         *
         * @param tier tier the machine is built for
         * @return an empty machine of that family and tier
         */
        public ElectricMachine machine(Voltage tier) {
            return factory.apply(Objects.requireNonNull(tier, "tier"));
        }
    }

    /**
     * Every family of the line, in the order the machines of it are registered.
     */
    public static final List<Family> ALL = List.of(
            ElectricFurnaceMachine.FAMILY,
            ElectricMaceratorMachine.FAMILY,
            ElectricCompressorMachine.FAMILY,
            ElectricExtractorMachine.FAMILY,
            ElectricForgeHammerMachine.FAMILY,
            ElectricAlloySmelterMachine.FAMILY);

    private MachineFamilies() {
        // Utility class: never instantiated.
    }

    /** Every family of the line, in the order the machines of it are registered. */
    public static List<Family> all() {
        return ALL;
    }
}
