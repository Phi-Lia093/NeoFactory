package com.philia093.neofactory.machine;

/**
 * The panel a machine is drawn in, which is the age the machine belongs to.
 * <p>
 * The sheet of the machine screens, {@code gui/machine_icons.png}, carries two panels of the very same size
 * and layout: the light grey one at the top and a bronze one right below it, which holds the same slots of
 * the player inventory in its lower half and is empty in its upper half where a machine stands its own
 * slots. A machine names the one it is drawn with when it is registered, see {@link MachineScreen#style()}.
 * <ul>
 *     <li>{@link #NORMAL} is the panel the machines of the electrical age are shown with - the light grey
 *         one, and the icons of the first columns of the sheet;</li>
 *     <li>{@link #BRONZE} is the panel of the age of steam: every machine that runs on steam is drawn with
 *         it, and its slots, its tanks and its progress bars are the bronze ones of the sheet, see
 *         {@link SlotKind} and {@link ProgressKind}.</li>
 * </ul>
 * <b>A plain slot is the slot of the panel itself.</b> The picture of the panel already carries the bevel a
 * slot is drawn with - of the player inventory and, for a style that has one, of the machine slots as well -
 * so a slot that names no picture of its own is cut out of the panel, see {@link #slotX()} and
 * {@link #slotY()}. The two coordinates are one whole slot picture, the same size as a cell of the icon grid,
 * and they are read by {@code MachineTextures}.
 */
public enum MachineStyle {

    /**
     * The age of electricity: the light grey panel at the top of the sheet.
     * <p>
     * Its plain slot is the picture of the grid as well, which is the cell {@code (0, 0)} of the icons - the
     * two are the very same pixels, so a machine of this style can be drawn from either.
     */
    NORMAL(0, 176, 0),

    /**
     * The age of steam: the bronze panel right below the grey one, with the bronze slots of the sheet.
     * <p>
     * Its plain slot is the first slot of its own inventory - the same cell of the panel the grey style takes
     * its slot from, only one panel lower in the sheet.
     */
    BRONZE(166, 7, 249);

    private final int panelY;
    private final int slotX;
    private final int slotY;

    MachineStyle(int panelY, int slotX, int slotY) {
        this.panelY = panelY;
        this.slotX = slotX;
        this.slotY = slotY;
    }

    /**
     * Upper edge of the panel of this style inside the sheet.
     * <p>
     * The panel is {@code 176} by {@code 166} pixels - the size every machine screen is designed for - and it
     * stands at the left edge of the sheet, so this is the only coordinate that tells the two panels apart.
     *
     * @return the coordinate in pixels
     */
    public int panelY() {
        return panelY;
    }

    /** Left edge of the plain slot of this style inside the sheet, in pixels. */
    public int slotX() {
        return slotX;
    }

    /** Upper edge of the plain slot of this style inside the sheet, in pixels. */
    public int slotY() {
        return slotY;
    }

    /** Name of this style, the name of the panel it is drawn with. */
    public String styleName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String toString() {
        return styleName();
    }
}
