package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.world.interaction.FaceRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The terminal of a machine: the plug of the line of the power in the colour of the age it was built in.
 * <p>
 * <b>A side that carries a job is drawn with the casing of the machine and the overlay of that job over
 * it</b> - the stub of a pipe for a fluid, the plug of the line of cables for the power - see
 * {@code FacePicture}. The plug of the power is the one overlay that says which age a machine belongs to:
 * <b>a machine of a later age wears the very same plug in the colour of that age</b>, the grey the industry
 * was drawn in for the low voltage, yellow for the middle one and orange for the high one, so a player reads
 * the tier of a line off the blocks at the end of it and not off a screen, see {@link MachineCasing}.
 * <p>
 * <b>The colour is a picture and not a tint of the drawing.</b> The three plugs are three pictures of the
 * pack, {@code machine_overlay/energy_in}, {@code machine_overlay/energy_in_mv} and
 * {@code machine_overlay/energy_in_hv}, and which one a side wears follows from the role of that side and
 * the tier of the machine: a machine that hangs on no line - a furnace that burns coal, a machine of the age
 * of steam - and the youngest age of the line are drawn with the picture the pack brought, and every later
 * age appends the name of its tier to it. The pictures are drawn from the grey one by
 * {@code tools/verify/import_energy_terminals.ps1}, which is why the shading of a plug survives its colour.
 * <p>
 * <b>A pipe is a pipe in every age.</b> Only the plug of the power is coloured: the stub of a pipe, the vent
 * of an exhaust and every other overlay are the same picture whatever tier a machine was built for, because
 * what the colour of a plug says is the tier of a <b>line</b> and a line is what carries the power.
 */
public final class MachineTerminals {

    private MachineTerminals() {
        // Utility class: never instantiated.
    }

    /**
     * Overlay a job of a machine is drawn with, in the colour of the age the machine was built in.
     *
     * @param role job of a side of a machine
     * @param tier tier the machine was built for, {@code null} for a machine that hangs on no line
     * @return the name of a picture relative to {@code blocks/}, empty for a side that carries no job
     */
    public static String overlayOf(FaceRole role, Voltage tier) {
        Objects.requireNonNull(role, "role");
        if (!role.isEnergy() || tier == null || !MachineFamilies.TIERS.contains(tier)
                || MachineFamilies.TIERS.get(0) == tier) {
            // A pipe, a vent and the plug of the youngest age of the line are drawn with the picture the pack
            // brought, see the note on this class.
            return role.overlay();
        }
        return role.overlay() + '_' + tier.fileName();
    }

    /**
     * Every picture a job of a machine is drawn with, in every age the industry is drawn in, each of them
     * once.
     * <p>
     * A side that is owned by a block entity is drawn with the picture of its job over the casing of the
     * block, and no model file names it: the mesher finds the layer of that picture in the array of pictures,
     * which is built from the models of the blocks and from this list, see {@code BlockPictures}.
     *
     * @return the pictures, in the order the roles and the ages are declared in
     */
    public static List<String> overlays() {
        List<String> pictures = new ArrayList<>();
        for (FaceRole role : FaceRole.values()) {
            for (Voltage tier : MachineFamilies.TIERS) {
                String picture = overlayOf(role, tier);
                if (!picture.isEmpty() && !pictures.contains(picture)) {
                    pictures.add(picture);
                }
            }
        }
        return List.copyOf(pictures);
    }

    @Override
    public String toString() {
        return "MachineTerminals(" + MachineFamilies.TIERS + ')';
    }
}
