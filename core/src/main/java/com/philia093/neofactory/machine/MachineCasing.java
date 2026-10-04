package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;

import java.util.Objects;

/**
 * The casing a machine of the line of the power is built of: the picture a side of it that carries a job is
 * drawn with.
 * <p>
 * Five of the six sides of a machine of the line are drawn by its model and carry the art of its family -
 * the mouth of a macerator, the gear over the top of it - see
 * {@link MachineFamilies.Family#pictureOf(Voltage, String)}. The side a player gave a job to is the sixth:
 * it is drawn by the block entity of the machine, which writes the casing of the machine with the overlay of
 * that job over it - the plug of the line of cables, the stub of a pipe - see {@code FacePicture} and
 * {@code MachineBlockEntity#pictureOn}.
 * <p>
 * <b>That casing is the one of the tier of the machine and never the one of the age of steam.</b> Every
 * family of a tier stands in the very same casing, so the plug of a macerator of the low voltage and the plug
 * of an alloy smelter of the low voltage are drawn over the same picture, and that picture is the casing the
 * generator of that tier is built of as well, see {@link TurbineTier#casing()}: a player who runs a line of
 * one age through a workshop reads one casing at every plug of it.
 * <p>
 * <b>The name comes from the tier.</b> The three casings of the industry are the three tiers of
 * {@link MachineFamilies#TIERS} spelled out, and a tier the game holds no art for is a tier no machine of the
 * line was drawn for: the question is refused instead of answered with the casing of another age, which is
 * what keeps the bronze of the age of steam off the plug of a machine of the line.
 */
public final class MachineCasing {

    private MachineCasing() {
        // Utility class: never instantiated.
    }

    /**
     * The picture of the casing of one tier, relative to {@code blocks/}.
     *
     * @param tier tier a machine of the line was built for, see {@link MachineFamilies#TIERS}
     * @return the name of the picture, such as {@code machine_lv/machine_lv}
     * @throws IllegalArgumentException when the game holds no casing of that tier
     */
    public static String pictureOf(Voltage tier) {
        Objects.requireNonNull(tier, "tier");
        if (!MachineFamilies.TIERS.contains(tier)) {
            throw new IllegalArgumentException("The " + tier.displayName()
                    + " is no tier of the line of the power: the industry has been drawn in three ages so "
                    + "far, see MachineFamilies#TIERS");
        }
        String name = "machine_" + tier.fileName();
        return name + '/' + name;
    }

    @Override
    public String toString() {
        return "MachineCasing(" + MachineFamilies.TIERS + ')';
    }
}
