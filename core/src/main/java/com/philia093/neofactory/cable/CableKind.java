package com.philia093.neofactory.cable;

import com.philia093.neofactory.pipe.PipeTexture;

/**
 * What the line of a cable is wrapped in: nothing, or a skin of insulation.
 * <p>
 * The industry draws every line twice: <b>{@link #WIRE}</b> is the bare line of a workshop that has just
 * learned to move energy, and <b>{@link #CABLE}</b> is the same line with a skin around it. The skin does
 * two things to a line:
 * <ul>
 *     <li><b>it halves the loss.</b> An insulated line loses <b>half of what the bare one loses, rounded
 *         down</b>, because the skin keeps the energy in the metal instead of letting it bleed into the
 *         ground: a copper line of four units a block loses two when it is wrapped, and a line of one unit
 *         a block loses nothing at all once it is insulated, see {@link #loss(CableMaterial)}};</li>
 *     <li><b>it changes the picture.</b> A bare line is drawn from the grey scale art of a metal tube and a
 *         wrapped one from the skin of the pack, see {@link #texture()}}.</li>
 * </ul>
 * <b>A skin is not a material.</b> Whatever a line is wrapped in, the line is still copper or silver: the
 * voltage it may carry and the material it is made of come from {@link CableMaterial} and the wrapping only
 * takes a part of its loss away. That is why a kind is no enum of materials and no second table: it is one
 * number and one picture, and every material of the table is sold both ways.
 * <p>
 * <b>The voltage stays where it was.</b> A wrapped line carries no more voltage than the bare one it is
 * made of, so a copper cable is a line of the middle voltage whether it is insulated or not, and the
 * machine at its end is the one that has to be able to take it.
 */
public enum CableKind {

    /** The bare line: a metal tube with nothing around it. */
    WIRE("wire", "Wire", PipeTexture.METAL.folder(), true),

    /**
     * The line with a skin around it: half the loss and the picture of the skin of the pack.
     * <p>
     * The pictures live in {@code assets/blocks/cable_insulation}, one per width of the line, and they are
     * brought in by {@code tools/import_cables.ps1}: a skin is no grey scale art of a metal, so it comes
     * with its own family and its own names, which are the names of the sizes of {@link CableSize}.
     */
    CABLE("cable", "Cable", "cable_insulation", true);

    private final String fileName;
    private final String displayName;
    private final String folder;
    private final boolean tinted;

    CableKind(String fileName, String displayName, String folder, boolean tinted) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.folder = folder;
        this.tinted = tinted;
    }

    /**
     * Name of this kind in lower case, the way it is written in files.
     *
     * @return {@code wire} or {@code cable}
     */
    public String fileName() {
        return fileName;
    }

    /** Name of this kind as a player reads it, {@code Wire} or {@code Cable}. */
    public String displayName() {
        return displayName;
    }

    /** Folder below {@code assets/blocks} the art of this kind lives in. */
    public String folder() {
        return folder;
    }

    /** {@code true} when the colour of the material is multiplied over the art of this kind. */
    public boolean isTinted() {
        return tinted;
    }

    /**
     * Picture of the line of a width, the fallback a block names while its model is not drawn.
     * <p>
     * A bare line is the end of the tube of its width in the metal family and a wrapped one is the skin of
     * its width, so the two kinds of the same width of the same material are two different pictures.
     *
     * @param size width of the line
     * @return the name of the picture, relative to {@code blocks/}
     */
    public String picture(CableSize size) {
        return this == WIRE ? PipeTexture.METAL.end(size.tube()) : folder + "/" + size.fileName();
    }

    /**
     * Energy one cable of a material of this kind takes away per block an energy travels.
     * <p>
     * A bare line loses what the table of the industry says its material loses; a line with a skin around
     * it loses <b>half of that, rounded down</b>. A line whose material loses one unit a block therefore
     * loses nothing once it is wrapped, which is what the table means by the cheap lines of the first ages.
     *
     * @param material material the line is made of
     * @return the loss in units of the game
     */
    public int loss(CableMaterial material) {
        return this == CABLE ? material.loss() / 2 : material.loss();
    }

    @Override
    public String toString() {
        return fileName;
    }
}
