package com.philia093.neofactory.pipe;

/**
 * The art a family of pipes is drawn with.
 * <p>
 * The pack draws one set of pipe pictures per family and every family holds the same eight files: the side
 * of a tube and the end of each of the seven {@link PipeSize sizes}. <b>All families but the wood are
 * identical grey scale</b>, which is what lets one set of models serve every metal: the colour of the
 * material is multiplied over the pictures, see {@link PipeMaterial#color()}. A new metal therefore needs
 * no art at all, and the wooden pipes - whose art is drawn in its own colours - are the one family that
 * brings its own files.
 */
public enum PipeTexture {

    /** Grey scale art painted with the colour of the material, the pipes of every metal. */
    METAL("pipe_metal", true),

    /** Art of its own, drawn in its own colours, the pipes of wood. */
    WOOD("pipe_wood", false);

    private final String folder;
    private final boolean tinted;

    PipeTexture(String folder, boolean tinted) {
        this.folder = folder;
        this.tinted = tinted;
    }

    /** Folder below {@code assets/blocks} that holds the pictures of this family. */
    public String folder() {
        return folder;
    }

    /** {@code true} when the colour of a material is multiplied over the pictures of this family. */
    public boolean isTinted() {
        return tinted;
    }

    /**
     * Picture of one part of a pipe.
     *
     * @param name name of the picture inside the folder, such as {@code side} or {@code medium}
     * @return the name of the picture, relative to {@code blocks/}
     */
    public String picture(String name) {
        return folder + "/" + name;
    }

    /** Picture of the side of a tube, the long face every size shares. */
    public String side() {
        return picture("side");
    }

    /**
     * Picture of the end of one size, the plate a tube's mouth is drawn with.
     *
     * @param size size to name
     * @return the name of the picture, relative to {@code blocks/}
     */
    public String end(PipeSize size) {
        return picture(size.fileName());
    }

    @Override
    public String toString() {
        return folder;
    }
}
